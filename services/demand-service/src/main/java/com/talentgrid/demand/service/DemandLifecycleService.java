package com.talentgrid.demand.service;

import com.talentgrid.demand.domain.entity.Demand;
import com.talentgrid.demand.domain.entity.DemandStatusHistory;
import com.talentgrid.demand.domain.enums.ClosureReason;
import com.talentgrid.demand.domain.enums.DemandStatus;
import com.talentgrid.demand.domain.statemachine.TransitionValidator;
import com.talentgrid.demand.dto.request.ApprovalRequest;
import com.talentgrid.demand.dto.request.StatusTransitionRequest;
import com.talentgrid.demand.dto.response.DemandResponse;
import com.talentgrid.demand.exception.DemandNotFoundException;
import com.talentgrid.demand.exception.InvalidDemandStateException;
import com.talentgrid.demand.kafka.producer.DemandEventProducer;
import com.talentgrid.demand.mapper.DemandMapper;
import com.talentgrid.demand.repository.DemandRepository;
import com.talentgrid.demand.repository.DemandStatusHistoryRepository;
import com.talentgrid.demand.util.SecurityUtils;
import com.talentgrid.audit.client.AuditLogClient;
import com.talentgrid.audit.dto.AuditAction;
import com.talentgrid.audit.dto.AuditLogPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Orchestrates demand lifecycle transitions including approval workflow,
 * status transitions, ON_HOLD resume, and audit trail logging.
 *
 * <p>Key behaviors:
 * <ul>
 *   <li>Approval auto-cascades: APPROVED → INTERNAL_SEARCH with searchStartAt set.</li>
 *   <li>ON_HOLD saves previousStatus; resume validates against it.</li>
 *   <li>Every transition writes a {@link DemandStatusHistory} audit record.</li>
 *   <li>Key transitions fire Kafka events via {@link DemandEventProducer}.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DemandLifecycleService {

    private final DemandRepository demandRepository;
    private final DemandStatusHistoryRepository historyRepository;
    private final TransitionValidator transitionValidator;
    private final DemandMapper demandMapper;
    private final DemandEventProducer eventProducer;
    private final AuditLogClient auditLogClient;

    /**
     * Handles approval or rejection of a demand in {@code PENDING_APPROVAL} status.
     *
     * <p>Valid decisions: APPROVED, DRAFT (reject), DUPLICATE, ON_HOLD, CANCELLED.
     * On APPROVED, auto-cascades to INTERNAL_SEARCH with timestamps set.
     *
     * @param id      the demand ID
     * @param request the approval request containing decision and comments
     * @return the updated demand as a response DTO
     */
    @Transactional
    public DemandResponse approve(Long id, ApprovalRequest request) {
        Demand demand = findActiveOrThrow(id);

        if (demand.getStatus() != DemandStatus.PENDING_APPROVAL) {
            throw new InvalidDemandStateException(
                    String.format("Demand %d is in %s, expected PENDING_APPROVAL for approval.",
                            id, demand.getStatus()));
        }

        DemandStatus decision = request.getDecision();

        // Determine closure reason for terminal decisions from approval
        ClosureReason closureReason = mapDecisionToClosureReason(decision);

        // Validate transition from PENDING_APPROVAL → decision
        transitionValidator.validate(demand, decision, closureReason);

        DemandStatus fromStatus = demand.getStatus();

        if (decision == DemandStatus.APPROVED) {
            // Set approval metadata
            demand.setApprovedAt(OffsetDateTime.now());
            demand.setApprovedBy(SecurityUtils.getCurrentUserId());
            demand.setApproverName(SecurityUtils.getCurrentUserName());
            
            if (request.getAssignedRecruiter() != null) {
                demand.setAssignedRecruiter(request.getAssignedRecruiter());
                demand.setAssignedRecruiterName(request.getAssignedRecruiterName());
            }
            if (request.getAssignedRm() != null) {
                demand.setAssignedRm(request.getAssignedRm());
                demand.setAssignedRmName(request.getAssignedRmName());
            }
            
            demand.setStatus(DemandStatus.APPROVED);
            writeHistory(demand, fromStatus, DemandStatus.APPROVED, null, request.getComments());

            // Auto-cascade to INTERNAL_SEARCH
            demand.setStatus(DemandStatus.INTERNAL_SEARCH);
            
            // Only auto-set searchStartAt if the creator didn't specify a custom future date
            if (demand.getSearchStartAt() == null) {
                demand.setSearchStartAt(OffsetDateTime.now());
            }
            
            writeHistory(demand, DemandStatus.APPROVED, DemandStatus.INTERNAL_SEARCH, null,
                    "Auto-transition on approval");

            eventProducer.publishApproved(demand);
        } else {
            // Handle ON_HOLD: save previous status for resume
            if (decision == DemandStatus.ON_HOLD) {
                demand.setPreviousStatus(demand.getStatus());
                demand.setClosureReason(ClosureReason.ON_HOLD.name());
            }
            if (decision == DemandStatus.DUPLICATE) {
                demand.setClosureReason(ClosureReason.DUPLICATE.name());
            }
            if (decision == DemandStatus.CANCELLED) {
                demand.setClosureReason(ClosureReason.CANCELLED.name());
            }

            demand.setStatus(decision);
            writeHistory(demand, fromStatus, decision,
                    closureReason != null ? closureReason.name() : null, request.getComments());

            if (decision == DemandStatus.CANCELLED) {
                eventProducer.publishCancelled(demand);
            } else if (decision == DemandStatus.ON_HOLD) {
                eventProducer.publishOnHold(demand);
            }
        }

        Demand saved = demandRepository.save(demand);

        // Publish audit event for approval decision
        auditLogClient.logAction(AuditLogPayload.builder()
                .entityType("DEMAND")
                .entityId(saved.getDemandId())
                .action(decision == DemandStatus.APPROVED ? AuditAction.APPROVE : AuditAction.STATUS_CHANGE)
                .actorId(SecurityUtils.getCurrentUserId())
                .beforeState(Map.of("status", fromStatus.name()))
                .afterState(Map.of("status", saved.getStatus().name()))
                .serviceName("demand-service")
                .endpoint("/api/demands/" + id + "/approve")
                .build());

        log.info("Demand approval decision '{}' processed for id={}", decision, id);
        return demandMapper.toResponse(saved);
    }

    /**
     * Performs a legal demand workflow transition.
     *
     * <p>Validates the transition using {@link TransitionValidator}, handles
     * ON_HOLD save/resume, updates fill counts and closure reason, writes
     * audit history, and fires Kafka events.
     *
     * @param id      the demand ID
     * @param request the status transition request
     * @return the updated demand as a response DTO
     */
    @Transactional
    public DemandResponse transitionStatus(Long id, StatusTransitionRequest request) {
        Demand demand = findActiveOrThrow(id);

        DemandStatus targetStatus = request.getTargetStatus();
        ClosureReason closureReason = request.getClosureReason();

        // Validate transition (matrix, RMG gate, closure reason, ON_HOLD resume guard)
        transitionValidator.validate(demand, targetStatus, closureReason);

        DemandStatus fromStatus = demand.getStatus();

        // ── Handle ON_HOLD: save previousStatus before transitioning ────────────
        if (targetStatus == DemandStatus.ON_HOLD) {
            demand.setPreviousStatus(demand.getStatus());
        }

        // ── Handle resume from ON_HOLD: clear previousStatus ────────────────────
        if (fromStatus == DemandStatus.ON_HOLD
                && (targetStatus == DemandStatus.INTERNAL_SEARCH
                    || targetStatus == DemandStatus.OPEN_EXTERNAL)) {
            demand.setPreviousStatus(null);
        }

        // ── Set closure reason on terminal/closure transitions ──────────────────
        if (closureReason != null) {
            demand.setClosureReason(closureReason.name());
        }

        // ── Set searchStartAt for INTERNAL_SEARCH (if not already set) ──────────
        if (targetStatus == DemandStatus.INTERNAL_SEARCH && demand.getSearchStartAt() == null) {
            demand.setSearchStartAt(OffsetDateTime.now());
        }

        // ── Transition the status ───────────────────────────────────────────────
        demand.setStatus(targetStatus);

        // ── Sync recruitedCount ─────────────────────────────────────────────────
        demand.setRecruitedCount(
                (demand.getInternalFilledCount() != null ? demand.getInternalFilledCount() : 0)
              + (demand.getExternalFilledCount() != null ? demand.getExternalFilledCount() : 0));

        // ── Write audit trail ───────────────────────────────────────────────────
        writeHistory(demand, fromStatus, targetStatus,
                closureReason != null ? closureReason.name() : null,
                request.getComments());

        Demand saved = demandRepository.save(demand);

        // ── Fire Kafka events ───────────────────────────────────────────────────
        publishEventForTransition(saved, targetStatus);

        // Publish audit event for status transition
        auditLogClient.logAction(AuditLogPayload.builder()
                .entityType("DEMAND")
                .entityId(saved.getDemandId())
                .action(AuditAction.STATUS_CHANGE)
                .actorId(SecurityUtils.getCurrentUserId())
                .beforeState(Map.of("status", fromStatus.name()))
                .afterState(Map.of("status", saved.getStatus().name()))
                .serviceName("demand-service")
                .endpoint("/api/demands/" + id + "/status")
                .build());

        log.info("Demand transitioned from {} to {} for id={}", fromStatus, targetStatus, id);
        return demandMapper.toResponse(saved);
    }

    // ─── Private helpers ─────────────────────────────────────────────────────────

    private Demand findActiveOrThrow(Long id) {
        return demandRepository.findByDemandIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new DemandNotFoundException(
                        "Demand not found with id: " + id));
    }

    private void writeHistory(Demand demand, DemandStatus from, DemandStatus to,
                              String closureReason, String comments) {
        DemandStatusHistory history = new DemandStatusHistory();
        history.setDemand(demand);
        history.setFromStatus(from);
        history.setToStatus(to);
        history.setClosureReason(closureReason);
        history.setComments(comments != null ? comments : "");
        history.setChangedAt(OffsetDateTime.now());
        history.setChangedBy(SecurityUtils.getCurrentUserId());
        historyRepository.save(history);
    }

    private ClosureReason mapDecisionToClosureReason(DemandStatus decision) {
        return switch (decision) {
            case DUPLICATE -> ClosureReason.DUPLICATE;
            case ON_HOLD -> ClosureReason.ON_HOLD;
            case CANCELLED -> ClosureReason.CANCELLED;
            default -> null;
        };
    }

    private void publishEventForTransition(Demand demand, DemandStatus targetStatus) {
        switch (targetStatus) {
            case PENDING_APPROVAL -> eventProducer.publishSubmitted(demand);
            case OPEN_EXTERNAL -> eventProducer.publishExternalOpened(demand);
            case FILLED_INTERNAL -> eventProducer.publishFilledInternal(demand);
            case FILLED_PARTIALLY -> eventProducer.publishFilledPartially(demand);
            case FILLED_EXTERNAL -> eventProducer.publishFilledExternal(demand);
            case CANCELLED -> eventProducer.publishCancelled(demand);
            case DUPLICATE -> eventProducer.publishDuplicate(demand);
            case CLOSED -> eventProducer.publishClosed(demand);
            case ON_HOLD -> eventProducer.publishOnHold(demand);
            case INTERNAL_SEARCH -> {
                // ON_HOLD → INTERNAL_SEARCH is a resume
                if (demand.getPreviousStatus() == DemandStatus.ON_HOLD) {
                    eventProducer.publishResumed(demand);
                }
            }
            default -> { /* No event for DRAFT or other internal transitions */ }
        }
    }
}
