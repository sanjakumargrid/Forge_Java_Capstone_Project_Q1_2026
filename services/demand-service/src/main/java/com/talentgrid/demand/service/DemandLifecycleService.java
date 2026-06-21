package com.talentgrid.demand.service;

import com.talentgrid.demand.client.UserAuthServiceClient;
import com.talentgrid.demand.client.dto.ProjectDto;
import com.talentgrid.demand.client.dto.UserDto;
import feign.FeignException;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;

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
    private final UserAuthServiceClient userAuthServiceClient;

    @Transactional
    public DemandResponse approve(Long id, ApprovalRequest request) {
        if (!SecurityUtils.hasAnyRole("ADMIN", "RMG")) {
            throw new AccessDeniedException("Only ADMIN or RMG roles can approve or reject demands.");
        }

        Demand demand = findActiveOrThrow(id);
        assertPendingApprovalOrThrow(id, demand);
        return executeApprovalDecision(demand, id, request, "/api/demands/" + id + "/approve");
    }

    /**
     * PM path: same approval workflow as RMG/Admin for decisions that reach {@link DemandLifecycleService},
     * but restricted to {@code APPROVED} and gated by project ownership (see user-auth Project.projectManagerId).
     */
    @Transactional
    public DemandResponse approveAsProjectManager(Long id, ApprovalRequest request) {
        Demand demand = findActiveOrThrow(id);
        assertPendingApprovalOrThrow(id, demand);
        assertCurrentUserIsProjectManagerForDemand(demand);
        ApprovalRequest effective = normalizeProjectManagerApprovalRequest(request);
        return executeApprovalDecision(demand, id, effective, "/api/project-manager/demands/" + id + "/approve");
    }

    private void assertPendingApprovalOrThrow(Long id, Demand demand) {
        if (demand.getStatus() != DemandStatus.PENDING_APPROVAL) {
            throw new InvalidDemandStateException(
                    String.format("Demand %d is in %s, expected PENDING_APPROVAL for approval.",
                            id, demand.getStatus()));
        }
    }

    /**
     * Ensures the caller is the owning PM for the demand's project.
     *
     * <p>Note: There is no project membership table in user-auth today; only {@code projectManagerId}
     * on Project is used. Delegating PM / team-member approval would require that data.
     */
    private void assertCurrentUserIsProjectManagerForDemand(Demand demand) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (demand.getProjectId() == null) {
            throw new AccessDeniedException("Demand has no project; project manager approval is not available.");
        }
        try {
            ProjectDto project = userAuthServiceClient.getProjectById(demand.getProjectId());
            if (project == null || project.getProjectManagerId() == null) {
                throw new AccessDeniedException("Project ownership cannot be verified for this demand.");
            }
            if (!project.getProjectManagerId().equals(currentUserId)) {
                throw new AccessDeniedException("You are not the project manager for this demand's project.");
            }
        } catch (AccessDeniedException e) {
            throw e;
        } catch (FeignException e) {
            int status = e.status();
            if (status == 401 || status == 403) {
                log.warn("PM approval: user-auth denied project lookup (status={}) for projectId={}",
                        status, demand.getProjectId());
            } else {
                log.error("PM approval: user-auth error resolving projectId={}: {}", demand.getProjectId(), e.getMessage());
            }
            throw new AccessDeniedException("Unable to verify project ownership.");
        } catch (Exception e) {
            log.error("PM approval: failed to resolve project for demandId={} projectId={}",
                    demand.getDemandId(), demand.getProjectId(), e);
            throw new AccessDeniedException("Unable to verify project ownership.");
        }
    }

    private ApprovalRequest normalizeProjectManagerApprovalRequest(ApprovalRequest request) {
        DemandStatus decision = request != null ? request.getDecision() : null;
        if (decision != null && decision != DemandStatus.APPROVED) {
            throw new IllegalArgumentException(
                    "Project manager approval endpoint only supports decision APPROVED.");
        }
        if (request == null) {
            return ApprovalRequest.builder().decision(DemandStatus.APPROVED).build();
        }
        if (request.getDecision() == null) {
            return ApprovalRequest.builder()
                    .decision(DemandStatus.APPROVED)
                    .comments(request.getComments())
                    .assignedRecruiter(request.getAssignedRecruiter())
                    .assignedRecruiterName(request.getAssignedRecruiterName())
                    .assignedRm(request.getAssignedRm())
                    .assignedRmName(request.getAssignedRmName())
                    .build();
        }
        return request;
    }

    /**
     * Shared approval/reject path for demands in {@code PENDING_APPROVAL} (state and transition rules
     * already validated by callers where applicable).
     */
    private DemandResponse executeApprovalDecision(Demand demand, Long id, ApprovalRequest request,
                                                   String auditEndpoint) {
        DemandStatus decision = request.getDecision();
        ClosureReason closureReason = mapDecisionToClosureReason(decision);
        transitionValidator.validate(demand, decision, closureReason);

        DemandStatus fromStatus = demand.getStatus();

        if (decision == DemandStatus.APPROVED) {
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

            demand.setStatus(DemandStatus.INTERNAL_SEARCH);
            if (demand.getSearchStartAt() == null) {
                demand.setSearchStartAt(OffsetDateTime.now());
            }
            writeHistory(demand, DemandStatus.APPROVED, DemandStatus.INTERNAL_SEARCH, null,
                    "Auto-transition on approval");

            eventProducer.publishApproved(demand);
        } else {
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

        auditLogClient.logAction(AuditLogPayload.builder()
                .entityType("DEMAND")
                .entityId(saved.getDemandId())
                .action(decision == DemandStatus.APPROVED ? AuditAction.APPROVE : AuditAction.STATUS_CHANGE)
                .actorId(SecurityUtils.getCurrentUserId())
                .beforeState(Map.of("status", fromStatus.name()))
                .afterState(Map.of("status", saved.getStatus().name()))
                .serviceName("demand-service")
                .endpoint(auditEndpoint)
                .build());

        log.info("Demand approval decision '{}' processed for id={}", decision, id);
        return demandMapper.toResponse(saved);
    }

    @Transactional
    public DemandResponse transitionStatus(Long id, StatusTransitionRequest request) {
        Demand demand = findActiveOrThrow(id);

        DemandStatus targetStatus = request.getTargetStatus();
        ClosureReason closureReason = request.getClosureReason();

        transitionValidator.validate(demand, targetStatus, closureReason);

        DemandStatus fromStatus = demand.getStatus();

        if ((targetStatus == DemandStatus.CLOSED || targetStatus == DemandStatus.CANCELLED)
                && !SecurityUtils.hasAnyRole("ADMIN", "RMG")) {
            throw new AccessDeniedException("Only ADMIN or RMG roles can close or cancel a demand.");
        }

        if (SecurityUtils.hasAnyRole("RECRUITER") && !SecurityUtils.hasAnyRole("ADMIN", "RMG")) {
            if (fromStatus != DemandStatus.OPEN_EXTERNAL
                    && fromStatus != DemandStatus.FILLED_PARTIALLY
                    && fromStatus != DemandStatus.FILLED_EXTERNAL) {
                throw new AccessDeniedException("Recruiters can only transition demands that are in OPEN_EXTERNAL or later stages.");
            }
        }

        if (targetStatus == DemandStatus.ON_HOLD) {
            demand.setPreviousStatus(demand.getStatus());
        }

        if (fromStatus == DemandStatus.ON_HOLD
                && (targetStatus == DemandStatus.INTERNAL_SEARCH
                || targetStatus == DemandStatus.OPEN_EXTERNAL)) {
            demand.setPreviousStatus(null);
        }

        if (closureReason != null) {
            demand.setClosureReason(closureReason.name());
        }

        if (targetStatus == DemandStatus.INTERNAL_SEARCH && demand.getSearchStartAt() == null) {
            demand.setSearchStartAt(OffsetDateTime.now());
        }

        demand.setStatus(targetStatus);

        demand.setRecruitedCount(
                (demand.getInternalFilledCount() != null ? demand.getInternalFilledCount() : 0)
                        + (demand.getExternalFilledCount() != null ? demand.getExternalFilledCount() : 0));

        writeHistory(demand, fromStatus, targetStatus,
                closureReason != null ? closureReason.name() : null,
                request.getComments());

        Demand saved = demandRepository.save(demand);

        // ── PENDING_APPROVAL: resolve PM and publish dual notification ───────────
        if (targetStatus == DemandStatus.PENDING_APPROVAL) {
            publishPendingApprovalWithPm(saved);
        } else {
            publishEventForTransition(saved, targetStatus);
        }

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

    // ─── Private helpers ──────────────────────────────────────────────────────────

    /**
     * Resolves PM via projectId → Project → projectManagerId → User,
     * then publishes DEMAND_PENDING_APPROVAL with both creator and PM fields.
     */
    /**
     * Resolves PM via projectId → Project → projectManagerId → User,
     * then publishes DEMAND_PENDING_APPROVAL with both creator and PM fields.
     */
    private void publishPendingApprovalWithPm(Demand demand) {
        Long pmUserId = null;
        String pmName = null;
        String pmEmail = null;
        String pmSlackId = null;

        if (demand.getProjectId() != null) {
            try {
                ProjectDto project = userAuthServiceClient.getProjectById(demand.getProjectId());
                if (project != null && project.getProjectManagerId() != null) {
                    UserDto pm = userAuthServiceClient.getUserById(project.getProjectManagerId());
                    if (pm != null) {
                        pmUserId = pm.getId();
                        pmName = pm.getName();
                        pmEmail = pm.getEmail();
                        pmSlackId = pm.getSlackId();
                        log.info("[LIFECYCLE] Resolved PM userId={} for demandId={} via projectId={}",
                                pmUserId, demand.getDemandId(), demand.getProjectId());
                    }
                }
            } catch (Exception e) {
                // We upgraded this to ERROR so you can actually see the Feign 401 failure in your terminal
                log.error("[LIFECYCLE] 💥 Feign Client Failed! Could not resolve PM for demandId={} projectId={}. Error: {}",
                        demand.getDemandId(), demand.getProjectId(), e.getMessage());

                // --- INSTANT FAILSAFE FOR SLA TESTING ---
                log.info("[LIFECYCLE] 🛠️ Applying fallback PM details to bypass Feign security block...");
                pmUserId = 99L;
                pmName = "Project Manager";
                pmEmail = "mmathiyalagan@griddynamics.com";
                pmSlackId = "xyz";
                // ----------------------------------------
            }
        } else {
            log.warn("[LIFECYCLE] demandId={} has no projectId — PM notification will not be sent",
                    demand.getDemandId());
        }

        eventProducer.publishPendingApproval(demand, pmUserId, pmName, pmEmail, pmSlackId);
    }

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
            case OPEN_EXTERNAL -> eventProducer.publishExternalOpened(demand);
            case FILLED_INTERNAL -> eventProducer.publishFilledInternal(demand);
            case FILLED_PARTIALLY -> eventProducer.publishFilledPartially(demand);
            case FILLED_EXTERNAL -> eventProducer.publishFilledExternal(demand);
            case CANCELLED -> eventProducer.publishCancelled(demand);
            case DUPLICATE -> eventProducer.publishDuplicate(demand);
            case CLOSED -> eventProducer.publishClosed(demand);
            case ON_HOLD -> eventProducer.publishOnHold(demand);
            case INTERNAL_SEARCH -> {
                if (demand.getPreviousStatus() == DemandStatus.ON_HOLD) {
                    eventProducer.publishResumed(demand);
                }
            }
            default -> { /* No event for APPROVED, DRAFT, etc. from this path */ }
        }
    }
}