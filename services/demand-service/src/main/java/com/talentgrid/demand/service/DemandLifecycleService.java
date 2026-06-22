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
     * Only the {@linkplain #assertCurrentUserIsProjectManagerForDemand project manager} for the demand's
     * {@code projectId} may act — same rules as {@link #approveAsProjectManager(Long, ApprovalRequest)}.
     * <p>Convenience alias for {@code POST /api/demands/{id}/approve}; prefer {@code PUT /api/project-manager/demands/{id}/approve} if you split routes by persona.
     */
    @Transactional
    public DemandResponse approve(Long id, ApprovalRequest request) {
        return approveAsProjectManager(id, request, "/api/demands/" + id + "/approve");
    }

    @Transactional
    public DemandResponse approveAsProjectManager(Long id, ApprovalRequest request) {
        return approveAsProjectManager(id, request, "/api/project-manager/demands/" + id + "/approve");
    }

    @Transactional
    protected DemandResponse approveAsProjectManager(Long id, ApprovalRequest request, String auditEndpoint) {
        Demand demand = findActiveOrThrow(id);
        assertPendingApprovalOrThrow(id, demand);
        assertCurrentUserIsProjectManagerForDemand(demand);
        ApprovalRequest effective = normalizeProjectManagerApprovalRequest(request);
        return executeApprovalDecision(demand, id, effective, auditEndpoint);
    }

    /**
     * HM submits for PM approval, or PM auto-approves from draft (same project).
     */
    @Transactional
    public DemandResponse submitDemand(Long id, String comments) {
        Demand demand = findActiveOrThrow(id);
        if (demand.getStatus() != DemandStatus.DRAFT) {
            throw new InvalidDemandStateException(
                    String.format("Demand %d is in %s, expected DRAFT for submit.", id, demand.getStatus()));
        }

        DemandStatus from = demand.getStatus();

        if (SecurityUtils.hasAnyRole("PROJECT_MANAGER")) {
            assertCurrentUserIsProjectManagerForDemand(demand);
            transitionValidator.validate(demand, DemandStatus.APPROVED, null);
            applyPostApprovalRouting(demand, from, comments, true);
        } else if (SecurityUtils.isHiringManager()) {
            if (!SecurityUtils.getCurrentUserId().equals(demand.getCreatedBy())) {
                throw new AccessDeniedException("Only the demand owner can submit for approval.");
            }
            transitionValidator.validate(demand, DemandStatus.PENDING_APPROVAL, null);
            demand.setStatus(DemandStatus.PENDING_APPROVAL);
            writeHistory(demand, from, DemandStatus.PENDING_APPROVAL, null, comments);
            Demand saved = demandRepository.save(demand);
            publishPendingApprovalWithPm(saved);
            auditStatusChange(saved, from, DemandStatus.PENDING_APPROVAL, "/api/demands/" + id + "/submit");
            return demandMapper.toResponse(saved);
        } else {
            throw new AccessDeniedException("Only HM or PM may submit a demand from draft.");
        }

        Demand saved = demandRepository.save(demand);
        auditStatusChange(saved, from, saved.getStatus(), "/api/demands/" + id + "/submit");
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
            applyPostApprovalRouting(demand, fromStatus, request.getComments(), true);
        } else {
            demand.setClosureReason(request.getClosureReason().name());
            demand.setStatus(DemandStatus.CLOSED);
            writeHistory(demand, fromStatus, DemandStatus.CLOSED,
                    request.getClosureReason().name(), request.getComments());
            eventProducer.publishClosed(demand);
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

    private void applyPostApprovalRouting(Demand demand, DemandStatus fromStatus, String comments, boolean publishEvents) {
        demand.setApprovedAt(OffsetDateTime.now());
        demand.setApprovedBy(SecurityUtils.getCurrentUserId());
        demand.setApproverName(SecurityUtils.getCurrentUserName());

        demand.setStatus(DemandStatus.APPROVED);
        writeHistory(demand, fromStatus, DemandStatus.APPROVED, null, comments);

        if (Boolean.TRUE.equals(demand.getBenchHiring())) {
            demand.setStatus(DemandStatus.OPEN_EXTERNAL);
            writeHistory(demand, DemandStatus.APPROVED, DemandStatus.OPEN_EXTERNAL, null,
                    "Bench hiring: skip internal search");
            if (publishEvents) {
                eventProducer.publishApproved(demand);
                eventProducer.publishExternalOpened(demand);
            }
        } else {
            demand.setStatus(DemandStatus.INTERNAL_SEARCH);
            if (demand.getSearchStartAt() == null) {
                demand.setSearchStartAt(OffsetDateTime.now());
            }
            writeHistory(demand, DemandStatus.APPROVED, DemandStatus.INTERNAL_SEARCH, null,
                    "Auto-transition on approval");
            if (publishEvents) {
                eventProducer.publishApproved(demand);
            }
        }
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
                .endpoint("/api/demands/" + id + "/status")
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
                .endpoint("/api/demands/" + id + "/status")
                .build());

        log.info("Demand {} filled and auto-closed from {}", id, fromStatus);
        return demandMapper.toResponse(saved);
    }

    private void assertTransitionPermissions(Demand demand, DemandStatus targetStatus, ClosureReason closureReason) {
        DemandStatus from = demand.getStatus();

        if (from == DemandStatus.DRAFT && targetStatus == DemandStatus.PENDING_APPROVAL) {
            if (!SecurityUtils.isHiringManager() && !SecurityUtils.hasAnyRole("ADMIN", "RMG")) {
                throw new AccessDeniedException("Only HM (or admin) may submit a demand for approval.");
            }
            if (!SecurityUtils.hasAnyRole("ADMIN", "RMG")
                    && !SecurityUtils.getCurrentUserId().equals(demand.getCreatedBy())) {
                throw new AccessDeniedException("Only the demand owner may submit for approval.");
            }
            return;
        }

        if (from == DemandStatus.ON_HOLD
                && (targetStatus == DemandStatus.INTERNAL_SEARCH
                || targetStatus == DemandStatus.OPEN_EXTERNAL)) {
            DemandStatus prev = demand.getPreviousStatus();
            if (prev == DemandStatus.INTERNAL_SEARCH
                    && !SecurityUtils.hasAnyRole("RESOURCE_MANAGER", "RM", "ADMIN", "RMG")) {
                throw new AccessDeniedException("Only RM (or admin) may resume internal search.");
            }
            if (prev == DemandStatus.OPEN_EXTERNAL
                    && !SecurityUtils.hasAnyRole("RECRUITER", "ADMIN", "RMG")) {
                throw new AccessDeniedException("Only recruiter (or admin) may resume external hiring.");
            }
            return;
        }

        if (targetStatus == DemandStatus.CLOSED && from == DemandStatus.ON_HOLD) {
            if (!SecurityUtils.hasAnyRole("RESOURCE_MANAGER", "RM", "ADMIN", "RMG")) {
                throw new AccessDeniedException("Only RM (or admin) may close an on-hold demand.");
            }
            if (closureReason != ClosureReason.RM_CLOSED_ON_HOLD) {
                throw new AccessDeniedException("Closing from ON_HOLD requires closureReason=RM_CLOSED_ON_HOLD.");
            }
            return;
        }

        if (targetStatus == DemandStatus.FILLED) {
            if (from == DemandStatus.INTERNAL_SEARCH) {
                if (!SecurityUtils.isHiringManager() && !SecurityUtils.hasAnyRole("ADMIN", "RMG")) {
                    throw new AccessDeniedException("Only HM (or admin) may accept internal fill.");
                }
            } else if (from == DemandStatus.OPEN_EXTERNAL) {
                if (!SecurityUtils.hasAnyRole("TA_MANAGER", "ADMIN", "RMG")) {
                    throw new AccessDeniedException("Only TA Manager (or admin) may approve external offer (FILLED).");
                }
                if (closureReason != ClosureReason.FILLED) {
                    throw new AccessDeniedException("External fill requires closureReason=FILLED.");
                }
            } else {
                throw new AccessDeniedException("FILLED is only valid from INTERNAL_SEARCH or OPEN_EXTERNAL.");
            }
            return;
        }

        if (targetStatus == DemandStatus.ON_HOLD && from == DemandStatus.INTERNAL_SEARCH) {
            if (!SecurityUtils.hasAnyRole("RESOURCE_MANAGER", "RM", "ADMIN", "RMG")) {
                throw new AccessDeniedException("Only RM (or admin) may place internal search on hold.");
            }
            return;
        }

        if (SecurityUtils.hasAnyRole("RECRUITER") && !SecurityUtils.hasAnyRole("ADMIN", "RMG")) {
            if (!(from == DemandStatus.OPEN_EXTERNAL
                    && (targetStatus == DemandStatus.ON_HOLD))) {
                throw new AccessDeniedException("Recruiters may only place OPEN_EXTERNAL demands on hold.");
            }
            return;
        }

        if ((targetStatus == DemandStatus.OPEN_EXTERNAL && from == DemandStatus.INTERNAL_SEARCH)
                || (targetStatus == DemandStatus.ON_HOLD && from == DemandStatus.INTERNAL_SEARCH)) {
            if (!SecurityUtils.hasAnyRole("RESOURCE_MANAGER", "RM", "ADMIN", "RMG")) {
                throw new AccessDeniedException("Only RM (or admin) may perform this internal-search transition.");
            }
            return;
        }

        if (targetStatus == DemandStatus.ON_HOLD && from == DemandStatus.OPEN_EXTERNAL) {
            if (!SecurityUtils.hasAnyRole("RECRUITER", "ADMIN", "RMG")) {
                throw new AccessDeniedException("Only recruiter (or admin) may hold external hiring.");
            }
            return;
        }

        if (SecurityUtils.hasAnyRole("ADMIN", "RMG")) {
            return;
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
                pmUserId = 99L;
                pmName = "Project Manager";
                pmEmail = "pm@example.com";
                pmSlackId = null;
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
                if (fromStatus == DemandStatus.INTERNAL_SEARCH) {
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
