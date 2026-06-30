package com.talentgrid.demand.service;

import com.talentgrid.demand.client.UserAuthServiceClient;
import com.talentgrid.demand.client.dto.ProjectDto;
import com.talentgrid.demand.client.dto.UserDto;
import com.talentgrid.demand.domain.entity.Demand;
import com.talentgrid.demand.domain.entity.DemandStatusHistory;
import com.talentgrid.demand.domain.enums.ClosureReason;
import com.talentgrid.demand.domain.enums.DemandStatus;
import com.talentgrid.demand.domain.enums.FillType;
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
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
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

    /**
     * Approve or reject a demand in {@code PENDING_APPROVAL}.
     * Only the portfolio manager for the demand's {@code projectId} may act
     * (platform {@code ADMIN} may override).
     * <p>Convenience alias for {@code POST /api/v1/demands/{id}/approve}; prefer {@code PUT /api/v1/portfolio-manager/demands/{id}/approve} if you split routes by persona.
     */
    @Transactional
    public DemandResponse approve(Long id, ApprovalRequest request) {
        return approveAsProjectManager(id, request, "/api/v1/demands/" + id + "/approve");
    }

    @Transactional
    public DemandResponse approveAsProjectManager(Long id, ApprovalRequest request) {
        return approveAsProjectManager(id, request, "/api/v1/portfolio-manager/demands/" + id + "/approve");
    }

    @Transactional
    protected DemandResponse approveAsProjectManager(Long id, ApprovalRequest request, String auditEndpoint) {
        Demand demand = findActiveOrThrow(id);
        assertPendingApprovalOrThrow(id, demand);
        if (!SecurityUtils.isPlatformAdmin() && !SecurityUtils.isResourceManager()) {
            assertCurrentUserIsPortfolioManagerForDemand(demand);
        }
        ApprovalRequest effective = normalizeProjectManagerApprovalRequest(request);
        return executeApprovalDecision(demand, id, effective, auditEndpoint);
    }

    /**
     * HM submits for PM approval, portfolio manager auto-approves from draft, or admin override.
     */
    @Transactional
    public DemandResponse submitDemand(Long id, String comments) {
        Demand demand = findActiveOrThrow(id);
        if (demand.getStatus() != DemandStatus.DRAFT) {
            throw new InvalidDemandStateException(
                    String.format("Demand %d is in %s, expected DRAFT for submit.", id, demand.getStatus()));
        }

        DemandStatus from = demand.getStatus();

        if (SecurityUtils.isPlatformAdmin()) {
            transitionValidator.validate(demand, DemandStatus.APPROVED, null);
            applyPostApprovalRouting(demand, from, comments);
        } else if (SecurityUtils.isPortfolioManager()) {
            assertCurrentUserIsPortfolioManagerForDemand(demand);
            transitionValidator.validate(demand, DemandStatus.APPROVED, null);
            applyPostApprovalRouting(demand, from, comments);
        } else if (SecurityUtils.isResourceManager()) {
            transitionValidator.validate(demand, DemandStatus.APPROVED, null);
            applyPostApprovalRouting(demand, from, comments);
        } else if (SecurityUtils.isHiringManager()) {
            if (!SecurityUtils.getCurrentUserId().equals(demand.getCreatedBy())) {
                throw new AccessDeniedException("Only the demand owner can submit for approval.");
            }
            transitionValidator.validate(demand, DemandStatus.PENDING_APPROVAL, null);
            demand.setStatus(DemandStatus.PENDING_APPROVAL);
            writeHistory(demand, from, DemandStatus.PENDING_APPROVAL, null, comments);
            Demand saved = demandRepository.save(demand);
            publishPendingApprovalWithPm(saved);
            auditStatusChange(saved, from, DemandStatus.PENDING_APPROVAL, "/api/v1/demands/" + id + "/submit");
            return demandMapper.toResponse(saved);
        } else {
            throw new AccessDeniedException("Only HM, portfolio manager, resource manager, or platform admin may submit a demand from draft.");
        }

        Demand saved = demandRepository.save(demand);
        eventProducer.publishApproved(saved);
        auditStatusChange(saved, from, saved.getStatus(), "/api/v1/demands/" + id + "/submit");
        return demandMapper.toResponse(saved);
    }

    private void validateApprovalRequest(ApprovalRequest request) {
        if (request == null || request.getDecision() == null) {
            throw new IllegalArgumentException("decision is required");
        }
        DemandStatus d = request.getDecision();
        if (d != DemandStatus.APPROVED && d != DemandStatus.CLOSED) {
            throw new IllegalArgumentException("decision must be APPROVED or CLOSED");
        }
        if (d == DemandStatus.CLOSED && request.getClosureReason() != ClosureReason.PM_REJECTED) {
            throw new IllegalArgumentException("Rejecting a demand requires closureReason=PM_REJECTED");
        }
    }

    private void assertPendingApprovalOrThrow(Long id, Demand demand) {
        if (demand.getStatus() != DemandStatus.PENDING_APPROVAL) {
            throw new InvalidDemandStateException(
                    String.format("Demand %d is in %s, expected PENDING_APPROVAL for approval.",
                            id, demand.getStatus()));
        }
    }

    private void assertCurrentUserIsPortfolioManagerForDemand(Demand demand) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (demand.getProjectId() == null) {
            throw new AccessDeniedException("Demand has no project; portfolio manager approval is not available.");
        }
        try {
            ProjectDto project = userAuthServiceClient.getProjectById(demand.getProjectId());
            if (project == null || project.getProjectManagerId() == null) {
                throw new AccessDeniedException("Project ownership cannot be verified for this demand.");
            }
            if (!project.getProjectManagerId().equals(currentUserId)) {
                throw new AccessDeniedException("You are not the portfolio manager for this demand's project.");
            }
        } catch (AccessDeniedException e) {
            throw e;
        } catch (FeignException e) {
            int status = e.status();
            if (status == 401 || status == 403) {
                log.warn("Portfolio manager approval: user-auth denied project lookup (status={}) for projectId={}",
                        status, demand.getProjectId());
            } else {
                log.error("Portfolio manager approval: user-auth error resolving projectId={}: {}",
                        demand.getProjectId(), e.getMessage());
            }
            throw new AccessDeniedException("Unable to verify project ownership.");
        } catch (Exception e) {
            log.error("Portfolio manager approval: failed to resolve project for demandId={} projectId={}",
                    demand.getDemandId(), demand.getProjectId(), e);
            throw new AccessDeniedException("Unable to verify project ownership.");
        }
    }

    private ApprovalRequest normalizeProjectManagerApprovalRequest(ApprovalRequest request) {
        DemandStatus decision = request != null ? request.getDecision() : null;
        if (decision == DemandStatus.CLOSED) {
            if (request.getClosureReason() != ClosureReason.PM_REJECTED) {
                throw new IllegalArgumentException("PM reject requires closureReason=PM_REJECTED");
            }
            return request;
        }
        if (decision != null && decision != DemandStatus.APPROVED) {
            throw new IllegalArgumentException("Project manager approval endpoint only supports APPROVED or CLOSED (reject).");
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

    private DemandResponse executeApprovalDecision(Demand demand, Long id, ApprovalRequest request,
                                                   String auditEndpoint) {
        DemandStatus decision = request.getDecision();
        ClosureReason closureReasonForValidation =
                decision == DemandStatus.CLOSED ? request.getClosureReason() : null;
        transitionValidator.validate(demand, decision, closureReasonForValidation);

        DemandStatus fromStatus = demand.getStatus();

        if (decision == DemandStatus.APPROVED) {
            if (request.getAssignedRecruiter() != null) {
                demand.setAssignedRecruiter(request.getAssignedRecruiter());
                demand.setAssignedRecruiterName(request.getAssignedRecruiterName());
            }
            if (request.getAssignedRm() != null) {
                demand.setAssignedRm(request.getAssignedRm());
                demand.setAssignedRmName(request.getAssignedRmName());
            }
            applyPostApprovalRouting(demand, fromStatus, request.getComments());
        } else {
            demand.setClosureReason(request.getClosureReason().name());
            demand.setStatus(DemandStatus.CLOSED);
            writeHistory(demand, fromStatus, DemandStatus.CLOSED,
                    request.getClosureReason().name(), request.getComments());
            eventProducer.publishClosed(demand);
        }

        Demand saved = demandRepository.save(demand);

        if (decision == DemandStatus.APPROVED) {
            eventProducer.publishApproved(saved);
        }

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

    /**
     * Marks a demand as {@code APPROVED} after PM sign-off. Search activation
     * ({@code APPROVED} → {@code INTERNAL_SEARCH} or bench {@code OPEN_EXTERNAL})
     * is performed asynchronously by {@link com.talentgrid.demand.scheduler.SearchActivationScheduler}.
     */
    private void applyPostApprovalRouting(Demand demand, DemandStatus fromStatus, String comments) {
        demand.setApprovedAt(OffsetDateTime.now());
        demand.setApprovedBy(SecurityUtils.getCurrentUserId());
        demand.setApproverName(SecurityUtils.getCurrentUserName());

        demand.setStatus(DemandStatus.APPROVED);
        writeHistory(demand, fromStatus, DemandStatus.APPROVED, null, comments);
    }

    /**
     * Activates all approved demands: {@code INTERNAL_SEARCH} by default, or
     * {@code OPEN_EXTERNAL} when {@code benchHiring} is true.
     *
     * @return number of demands transitioned
     */
    @Transactional
    public int activateApprovedDemands() {
        List<Demand> approvedDemands =
                demandRepository.findByStatusAndIsDeletedFalse(DemandStatus.APPROVED);

        if (approvedDemands.isEmpty()) {
            return 0;
        }

        int activated = 0;
        for (Demand demand : approvedDemands) {
            try {
                if (activateApprovedDemand(demand)) {
                    activated++;
                }
            } catch (Exception ex) {
                log.error("[SEARCH-ACTIVATION] Failed for demandId={}", demand.getDemandId(), ex);
            }
        }
        return activated;
    }

    /**
     * Transitions a single {@code APPROVED} demand into active search.
     */
    @Transactional
    public boolean activateApprovedDemand(Demand demand) {
        if (demand.getStatus() != DemandStatus.APPROVED) {
            log.debug("[SEARCH-ACTIVATION] demandId={} is {} — skipping",
                    demand.getDemandId(), demand.getStatus());
            return false;
        }

        DemandStatus targetStatus = Boolean.TRUE.equals(demand.getBenchHiring())
                ? DemandStatus.OPEN_EXTERNAL
                : DemandStatus.INTERNAL_SEARCH;

        transitionValidator.validate(demand, targetStatus, null);

        DemandStatus fromStatus = demand.getStatus();
        if (targetStatus == DemandStatus.INTERNAL_SEARCH && demand.getSearchStartAt() == null) {
            demand.setSearchStartAt(OffsetDateTime.now());
        }

        demand.setStatus(targetStatus);
        String activationComment = targetStatus == DemandStatus.OPEN_EXTERNAL
                ? "Bench hiring: skip internal search (auto-activated)"
                : "Auto-activated internal search";
        writeHistory(demand, fromStatus, targetStatus, null, activationComment, 0L);

        demandRepository.save(demand);

        if (targetStatus == DemandStatus.OPEN_EXTERNAL) {
            eventProducer.publishExternalOpened(demand);
        }

        log.info("[SEARCH-ACTIVATION] demandId={} transitioned {} → {}",
                demand.getDemandId(), fromStatus, targetStatus);
        return true;
    }

    @Transactional
    public DemandResponse transitionStatus(Long id, StatusTransitionRequest request) {
        Demand demand = findActiveOrThrow(id);

        DemandStatus targetStatus = request.getTargetStatus();
        ClosureReason closureReason = request.getClosureReason();

        assertTransitionPermissions(demand, targetStatus, closureReason);

        transitionValidator.validate(demand, targetStatus, closureReason);

        DemandStatus fromStatus = demand.getStatus();

        if (targetStatus == DemandStatus.ON_HOLD) {
            demand.setPreviousStatus(demand.getStatus());
        }

        if (fromStatus == DemandStatus.ON_HOLD
                && (targetStatus == DemandStatus.INTERNAL_SEARCH
                || targetStatus == DemandStatus.OPEN_EXTERNAL)) {
            demand.setPreviousStatus(null);
        }

        if (targetStatus == DemandStatus.FILLED) {
            return applyFilledWithAutoClose(demand, fromStatus, closureReason, request.getComments(), id);
        }

        if (closureReason != null) {
            demand.setClosureReason(closureReason.name());
        }

        if (targetStatus == DemandStatus.INTERNAL_SEARCH && demand.getSearchStartAt() == null) {
            demand.setSearchStartAt(OffsetDateTime.now());
        }

        demand.setStatus(targetStatus);

        writeHistory(demand, fromStatus, targetStatus,
                closureReason != null ? closureReason.name() : null,
                request.getComments());

        Demand saved = demandRepository.save(demand);

        if (targetStatus == DemandStatus.PENDING_APPROVAL) {
            publishPendingApprovalWithPm(saved);
        } else {
            publishEventForTransition(saved, fromStatus, targetStatus);
        }

        auditLogClient.logAction(AuditLogPayload.builder()
                .entityType("DEMAND")
                .entityId(saved.getDemandId())
                .action(AuditAction.STATUS_CHANGE)
                .actorId(SecurityUtils.getCurrentUserId())
                .beforeState(Map.of("status", fromStatus.name()))
                .afterState(Map.of("status", saved.getStatus().name()))
                .serviceName("demand-service")
                .endpoint("/api/v1/demands/" + id + "/status")
                .build());

        log.info("Demand transitioned from {} to {} for id={}", fromStatus, targetStatus, id);
        return demandMapper.toResponse(saved);
    }

    /**
     * Completes internal fill (e.g. HM accepted nomination) — {@code FILLED} then auto {@code CLOSED}.
     */
    @Transactional
    public DemandResponse fillDemandInternally(Long demandId, String comments) {
        Demand demand = findActiveOrThrow(demandId);
        assertTransitionPermissions(demand, DemandStatus.FILLED, ClosureReason.FILLED);
        transitionValidator.validate(demand, DemandStatus.FILLED, ClosureReason.FILLED);
        return applyFilledWithAutoClose(demand, demand.getStatus(), ClosureReason.FILLED, comments, demandId);
    }

    private DemandResponse applyFilledWithAutoClose(Demand demand, DemandStatus fromStatus,
                                                    ClosureReason fillReason, String comments, Long id) {
        demand.setIsFilled(true);
        demand.setFillType(fromStatus == DemandStatus.INTERNAL_SEARCH ? FillType.INTERNAL : FillType.EXTERNAL);
        demand.setClosureReason(fillReason.name());
        demand.setStatus(DemandStatus.FILLED);
        writeHistory(demand, fromStatus, DemandStatus.FILLED, fillReason.name(), comments);
        eventProducer.publishDemandFilled(demand);

        transitionValidator.validate(demand, DemandStatus.CLOSED, ClosureReason.AUTO_CLOSED_AFTER_FILL);
        demand.setClosureReason(ClosureReason.AUTO_CLOSED_AFTER_FILL.name());
        demand.setStatus(DemandStatus.CLOSED);
        writeHistory(demand, DemandStatus.FILLED, DemandStatus.CLOSED,
                ClosureReason.AUTO_CLOSED_AFTER_FILL.name(), "Auto-close after filled");
        eventProducer.publishClosed(demand);

        Demand saved = demandRepository.save(demand);

        auditLogClient.logAction(AuditLogPayload.builder()
                .entityType("DEMAND")
                .entityId(saved.getDemandId())
                .action(AuditAction.STATUS_CHANGE)
                .actorId(SecurityUtils.getCurrentUserId())
                .beforeState(Map.of("status", fromStatus.name()))
                .afterState(Map.of("status", saved.getStatus().name()))
                .serviceName("demand-service")
                .endpoint("/api/v1/demands/" + id + "/status")
                .build());

        log.info("Demand {} filled and auto-closed from {}", id, fromStatus);
        return demandMapper.toResponse(saved);
    }

    private void assertTransitionPermissions(Demand demand, DemandStatus targetStatus, ClosureReason closureReason) {
        if (SecurityUtils.canManageDemandLifecycle()) {
            return;
        }

        DemandStatus from = demand.getStatus();

        if (from == DemandStatus.DRAFT && targetStatus == DemandStatus.PENDING_APPROVAL) {
            if (!SecurityUtils.isHiringManager()) {
                throw new AccessDeniedException("Only the hiring manager may submit a demand for approval.");
            }
            if (!SecurityUtils.getCurrentUserId().equals(demand.getCreatedBy())) {
                throw new AccessDeniedException("Only the demand owner may submit for approval.");
            }
            return;
        }

        if (from == DemandStatus.INTERNAL_SEARCH
                && targetStatus == DemandStatus.OPEN_EXTERNAL
                && closureReason == ClosureReason.HM_REJECTED_NOMINATION) {
            if (!SecurityUtils.isHiringManager()) {
                throw new AccessDeniedException("Only HM may open external search after rejecting a nomination.");
            }
            return;
        }

        if (targetStatus == DemandStatus.FILLED && from == DemandStatus.INTERNAL_SEARCH) {
            if (!SecurityUtils.isHiringManager()) {
                throw new AccessDeniedException("Only HM may accept internal fill.");
            }
            return;
        }

        if (targetStatus == DemandStatus.FILLED && from == DemandStatus.OPEN_EXTERNAL) {
            if (!SecurityUtils.isTaManager()) {
                throw new AccessDeniedException("Only TA Manager may approve external offer (FILLED).");
            }
            if (closureReason != ClosureReason.FILLED) {
                throw new AccessDeniedException("External fill requires closureReason=FILLED.");
            }
            return;
        }

        if (targetStatus == DemandStatus.FILLED) {
            throw new AccessDeniedException("FILLED is only valid from INTERNAL_SEARCH or OPEN_EXTERNAL.");
        }

        if (from == DemandStatus.ON_HOLD && targetStatus == DemandStatus.INTERNAL_SEARCH) {
            if (!SecurityUtils.isResourceManager()) {
                throw new AccessDeniedException("Only RM may resume internal search.");
            }
            return;
        }

        if (from == DemandStatus.ON_HOLD && targetStatus == DemandStatus.OPEN_EXTERNAL) {
            if (!SecurityUtils.isRecruiter()) {
                throw new AccessDeniedException("Only recruiter may resume external hiring.");
            }
            return;
        }

        if (targetStatus == DemandStatus.CLOSED && from == DemandStatus.ON_HOLD) {
            if (!SecurityUtils.isResourceManager()) {
                throw new AccessDeniedException("Only RM may close an on-hold demand.");
            }
            if (closureReason != ClosureReason.RM_CLOSED_ON_HOLD) {
                throw new AccessDeniedException("Closing from ON_HOLD requires closureReason=RM_CLOSED_ON_HOLD.");
            }
            return;
        }

        if (from == DemandStatus.INTERNAL_SEARCH
                && (targetStatus == DemandStatus.ON_HOLD || targetStatus == DemandStatus.OPEN_EXTERNAL)) {
            if (!SecurityUtils.isResourceManager()) {
                throw new AccessDeniedException("Only RM may perform this internal-search transition.");
            }
            return;
        }

        if (from == DemandStatus.OPEN_EXTERNAL && targetStatus == DemandStatus.ON_HOLD) {
            if (!SecurityUtils.isRecruiter()) {
                throw new AccessDeniedException("Only recruiter may hold external hiring.");
            }
            return;
        }

        if (SecurityUtils.isRecruiter()) {
            throw new AccessDeniedException(
                    "Recruiters may only place OPEN_EXTERNAL demands on hold or resume external hiring.");
        }

        throw new AccessDeniedException("Insufficient permissions for this transition.");
    }

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
                log.error("[LIFECYCLE] Could not resolve PM for demandId={} projectId={}: {}",
                        demand.getDemandId(), demand.getProjectId(), e.getMessage());
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
        writeHistory(demand, from, to, closureReason, comments, SecurityUtils.getCurrentUserId());
    }

    private void writeHistory(Demand demand, DemandStatus from, DemandStatus to,
                              String closureReason, String comments, Long changedBy) {
        DemandStatusHistory history = new DemandStatusHistory();
        history.setDemand(demand);
        history.setFromStatus(from);
        history.setToStatus(to);
        history.setClosureReason(closureReason);
        history.setComments(comments != null ? comments : "");
        history.setChangedAt(OffsetDateTime.now());
        history.setChangedBy(changedBy);
        historyRepository.save(history);
    }

    private void auditStatusChange(Demand saved, DemandStatus from, DemandStatus to, String endpoint) {
        auditLogClient.logAction(AuditLogPayload.builder()
                .entityType("DEMAND")
                .entityId(saved.getDemandId())
                .action(AuditAction.STATUS_CHANGE)
                .actorId(SecurityUtils.getCurrentUserId())
                .beforeState(Map.of("status", from.name()))
                .afterState(Map.of("status", to.name()))
                .serviceName("demand-service")
                .endpoint(endpoint)
                .build());
    }

    private void publishEventForTransition(Demand demand, DemandStatus fromStatus, DemandStatus targetStatus) {
        switch (targetStatus) {
            case OPEN_EXTERNAL -> {
                if (fromStatus == DemandStatus.INTERNAL_SEARCH || fromStatus == DemandStatus.ON_HOLD) {
                    eventProducer.publishExternalOpened(demand);
                }
            }
            case CLOSED -> eventProducer.publishClosed(demand);
            case ON_HOLD -> eventProducer.publishOnHold(demand);
            case INTERNAL_SEARCH -> {
                if (fromStatus == DemandStatus.ON_HOLD) {
                    eventProducer.publishResumed(demand);
                }
            }
            default -> { /* e.g. PENDING_APPROVAL handled elsewhere */ }
        }
    }
}
