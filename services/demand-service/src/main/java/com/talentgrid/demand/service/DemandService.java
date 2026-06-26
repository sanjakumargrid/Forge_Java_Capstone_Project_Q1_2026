package com.talentgrid.demand.service;

import com.talentgrid.demand.domain.entity.Demand;
import com.talentgrid.demand.domain.entity.DemandStatusHistory;
import com.talentgrid.demand.domain.enums.DemandStatus;
import com.talentgrid.demand.domain.enums.EmploymentType;
import com.talentgrid.demand.domain.enums.WorkMode;
import com.talentgrid.demand.dto.request.DemandRequest;
import com.talentgrid.demand.dto.response.DemandResponse;
import com.talentgrid.demand.exception.DemandNotFoundException;
import com.talentgrid.demand.exception.InvalidDemandStateException;
import com.talentgrid.demand.mapper.DemandMapper;
import com.talentgrid.demand.repository.DemandRepository;
import com.talentgrid.demand.repository.DemandStatusHistoryRepository;
import com.talentgrid.demand.util.SecurityUtils;
import com.talentgrid.demand.client.UserAuthServiceClient;
import com.talentgrid.audit.client.AuditLogClient;
import com.talentgrid.audit.dto.AuditAction;
import com.talentgrid.audit.dto.AuditLogPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.talentgrid.demand.domain.entity.JobTitle;
import com.talentgrid.demand.domain.entity.Skill;
import com.talentgrid.demand.domain.entity.DemandSkill;

/**
 * Handles demand CRUD operations (create, update, soft-delete).
 *
 * <p>Business rules enforced:
 * <ul>
 * <li>New demands are always created in {@code DRAFT} status.</li>
 * <li>Updates are permitted when the demand is in any of
 *     {@code DRAFT}, {@code PENDING_APPROVAL}, {@code APPROVED},
 *     {@code INTERNAL_SEARCH}, {@code OPEN_EXTERNAL}, or {@code ON_HOLD}.</li>
 * <li>Updates are blocked when the demand is in {@code FILLED} or
 *     {@code CLOSED} status.</li>
 * <li>Once a demand reaches {@code APPROVED} status, the following fields
 *     are locked and cannot be edited: Role Title, Client Account,
 *     Business Unit, Project, Priority, Seniority Level, Employment Type,
 *     Department.</li>
 * <li>Every update must include a non-blank {@code reasonForEdit}, which is
 *     persisted to the edit history for audit purposes.</li>
 * <li>Soft-delete (setting {@code is_deleted = true}) is only permitted in
 *     {@code DRAFT} status.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DemandService {

    /** Statuses that allow any form of field editing. */
    private static final Set<DemandStatus> EDITABLE_STATUSES = EnumSet.of(
            DemandStatus.DRAFT,
            DemandStatus.PENDING_APPROVAL,
            DemandStatus.APPROVED,
            DemandStatus.INTERNAL_SEARCH,
            DemandStatus.OPEN_EXTERNAL,
            DemandStatus.ON_HOLD);

    /**
     * Statuses at or past APPROVED where a subset of fields become locked
     * (Role Title, Client Account, Business Unit, Project, Priority,
     * Seniority Level, Employment Type, Department).
     */
    private static final Set<DemandStatus> POST_APPROVAL_STATUSES = EnumSet.of(
            DemandStatus.APPROVED,
            DemandStatus.INTERNAL_SEARCH,
            DemandStatus.OPEN_EXTERNAL,
            DemandStatus.ON_HOLD);

    private final DemandRepository demandRepository;
    private final DemandStatusHistoryRepository demandStatusHistoryRepository;
    private final DemandMapper demandMapper;
    private final DemandValidationService validationService;
    private final AuditLogClient auditLogClient;
    private final UserAuthServiceClient userAuthServiceClient;
    private final JobTitleLookupService jobTitleLookupService;
    private final SkillLookupService skillLookupService;

    /**
     * Creates a new workforce demand in {@code DRAFT} status.
     *
     * @param request the create request containing demand fields
     * @return the persisted demand as a response DTO
     */
    @Transactional
    public DemandResponse createDemand(DemandRequest request) {
        validationService.validateCreate(request);
        Demand demand = demandMapper.toEntity(request);

        JobTitle jobTitle = jobTitleLookupService.resolveJobTitleId(request.getJobTitleId());
        demand.setTitle(jobTitle.getTitleName());

        // Default onboarding date to target date if not provided
        if (demand.getOnboardingDate() == null) {
            demand.setOnboardingDate(demand.getTargetDate());
        }

        enrichDemandFromReferences(demand, request);
        applyCreateDefaults(demand);

        // Extract the user ID, email, and name from the security context and set it as
        // the creator
        demand.setCreatedBy(SecurityUtils.getCurrentUserId());
        demand.setCreatorEmail(SecurityUtils.getCurrentUserEmail());
        demand.setCreatorName(SecurityUtils.getCurrentUserName());
        Long creatorId = SecurityUtils.getCurrentUserId();
        var user = userAuthServiceClient.getUserById(creatorId);
        if (user != null && user.getSlackId() != null) {
            demand.setCreatorSlackId(user.getSlackId());
        }

        Demand saved = demandRepository.save(demand);

        List<DemandSkill> demandSkills = createDemandSkills(saved, request.getMandatorySkillIds(), request.getOptionalSkillIds());
        if (!demandSkills.isEmpty()) {
            saved.setDemandSkills(new ArrayList<>(demandSkills));
            saved = demandRepository.save(saved);
        }

        // Publish audit event
        auditLogClient.logAction(AuditLogPayload.builder()
                .entityType("DEMAND")
                .entityId(saved.getDemandId())
                .action(AuditAction.CREATE)
                .actorId(SecurityUtils.getCurrentUserId())
                .beforeState(null)
                .afterState(Map.of(
                        "title", saved.getTitle(),
                        "status", saved.getStatus().name()))
                .serviceName("demand-service")
                .endpoint("/api/v1/demands")
                .build());

        log.info("Demand created successfully with id={}", saved.getDemandId());
        return demandMapper.toResponse(saved);
    }

    /**
     * Updates editable fields of an existing demand.
     *
     * <p>Editing is allowed when the demand status is one of:
     * {@code DRAFT}, {@code PENDING_APPROVAL}, {@code APPROVED},
     * {@code INTERNAL_SEARCH}, {@code OPEN_EXTERNAL}, {@code ON_HOLD}.
     *
     * <p>Once a demand has reached {@code APPROVED} status the following fields
     * are locked and will be rejected if present in the request:
     * Role Title ({@code jobTitleId/title}), Client Account ({@code accountId}),
     * Business Unit ({@code businessUnit}), Project ({@code projectId}),
     * Priority ({@code priority}), Seniority Level ({@code level}),
     * Employment Type ({@code employmentType}), Department ({@code department}).
     *
     * <p>{@code reasonForEdit} is mandatory and is persisted to the edit-history
     * table for audit purposes.
     *
     * @param id      the demand ID
     * @param request the partial update request (null fields are ignored)
     * @return the updated demand as a response DTO
     * @throws DemandNotFoundException     if the demand does not exist or is soft-deleted
     * @throws InvalidDemandStateException if the demand status does not permit editing,
     *                                     or if locked fields are sent for a post-approval demand
     */
    @Transactional
    public DemandResponse updateDemand(Long id, DemandRequest request) {
        validationService.validateUpdate(request);
        Demand demand = findActiveOrThrow(id);
        requireEditableState(demand);
        assertLockedFieldsNotModified(request, demand);

        Map<String, Object> beforeState = snapshotDemand(demand);

        if (request.getJobTitleId() != null) {
            JobTitle jobTitle = jobTitleLookupService.resolveJobTitleId(request.getJobTitleId());
            demand.setTitle(jobTitle.getTitleName());
        }

        demandMapper.applyUpdate(request, demand);

        if (request.getMandatorySkillIds() != null || request.getOptionalSkillIds() != null) {
            syncDemandSkills(demand, request.getMandatorySkillIds(), request.getOptionalSkillIds());
        }

        Demand saved = demandRepository.save(demand);

        Map<String, Object> afterState = snapshotDemand(saved);

        persistEditHistory(saved, request.getReasonForEdit());

        auditLogClient.logAction(AuditLogPayload.builder()
                .entityType("DEMAND")
                .entityId(saved.getDemandId())
                .action(AuditAction.UPDATE)
                .actorId(SecurityUtils.getCurrentUserId())
                .reasonForEdit(request.getReasonForEdit())
                .beforeState(beforeState)
                .afterState(afterState)
                .serviceName("demand-service")
                .endpoint("/api/v1/demands/" + id)
                .build());

        log.info("Demand updated successfully with id={}", saved.getDemandId());
        return demandMapper.toResponse(saved);
    }

    /**
     * Soft-deletes a demand by setting {@code is_deleted = true}.
     * Only permitted when the demand is in {@code DRAFT} status.
     *
     * @param id the demand ID
     * @throws DemandNotFoundException     if the demand does not exist or is
     *                                     already deleted
     * @throws InvalidDemandStateException if the demand is not in {@code DRAFT}
     *                                     status
     */
    @Transactional
    public void deleteDemand(Long id) {
        Demand demand = findActiveOrThrow(id);
        requireDraftState(demand, "delete");

        demand.setIsDeleted(true);
        demandRepository.save(demand);

        // Publish audit event
        auditLogClient.logAction(AuditLogPayload.builder()
                .entityType("DEMAND")
                .entityId(demand.getDemandId())
                .action(AuditAction.DELETE)
                .actorId(SecurityUtils.getCurrentUserId())
                .afterState(Map.of("isDeleted", true))
                .serviceName("demand-service")
                .endpoint("/api/v1/demands/" + id)
                .build());

        log.info("Demand soft-deleted successfully with id={}", id);
    }

    // ─── Private helpers ─────────────────────────────────────────────────────────

    /**
     * Finds a non-deleted demand by ID or throws {@link DemandNotFoundException}.
     */
    private Demand findActiveOrThrow(Long id) {
        return demandRepository.findByDemandIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new DemandNotFoundException(
                        "Demand not found with id: " + id));
    }

    /**
     * Asserts the demand is in {@code DRAFT} status. Used for soft-delete.
     */
    private void requireDraftState(Demand demand, String operation) {
        if (demand.getStatus() != DemandStatus.DRAFT) {
            throw new InvalidDemandStateException(
                    String.format("Cannot %s demand (id=%d): current status is %s, expected DRAFT.",
                            operation, demand.getDemandId(), demand.getStatus()));
        }
    }

    /**
     * Asserts the demand is in an editable status for field updates.
     * Editing is allowed for: DRAFT, PENDING_APPROVAL, APPROVED,
     * INTERNAL_SEARCH, OPEN_EXTERNAL, ON_HOLD.
     * Throws {@link InvalidDemandStateException} for FILLED or CLOSED.
     */
    private void requireEditableState(Demand demand) {
        if (!EDITABLE_STATUSES.contains(demand.getStatus())) {
            throw new InvalidDemandStateException(
                    String.format("Cannot update demand (id=%d): editing is not allowed in status %s. " +
                            "Allowed statuses: DRAFT, PENDING_APPROVAL, APPROVED, INTERNAL_SEARCH, OPEN_EXTERNAL, ON_HOLD.",
                            demand.getDemandId(), demand.getStatus()));
        }
    }

    /**
     * Validates that no locked fields are included in the request when the demand
     * has already reached or passed {@code APPROVED} status.
     *
     * <p>Locked fields: Role Title ({@code jobTitleId/title}), Client Account
     * ({@code accountId}), Business Unit ({@code businessUnit}), Project
     * ({@code projectId}), Priority ({@code priority}), Seniority Level
     * ({@code level}), Employment Type ({@code employmentType}),
     * Department ({@code department}).
     */
    private void assertLockedFieldsNotModified(DemandRequest request, Demand demand) {
        if (!POST_APPROVAL_STATUSES.contains(demand.getStatus())) {
            return;
        }
        List<String> violations = new ArrayList<>();
        if (request.getJobTitleId() != null || request.getTitle() != null) {
            violations.add("Role Title (jobTitleId/title)");
        }
        if (request.getAccountId() != null) {
            violations.add("Client Account (accountId)");
        }
        if (request.getBusinessUnit() != null) {
            violations.add("Business Unit (businessUnit)");
        }
        if (request.getProjectId() != null) {
            violations.add("Project Name (projectId)");
        }
        if (request.getPriority() != null) {
            violations.add("Priority");
        }
        if (request.getLevel() != null) {
            violations.add("Seniority Level (level)");
        }
        if (request.getEmploymentType() != null) {
            violations.add("Employment Type");
        }
        if (request.getDepartment() != null) {
            violations.add("Department");
        }
        if (!violations.isEmpty()) {
            throw new InvalidDemandStateException(
                    String.format("Cannot modify locked field(s) for demand (id=%d) in status %s: %s.",
                            demand.getDemandId(), demand.getStatus(), String.join(", ", violations)));
        }
    }

    /**
     * Writes a {@link DemandStatusHistory} record to capture an edit event
     * (non-status-change) with the caller's reason. The {@code fromStatus} and
     * {@code toStatus} are both set to the current demand status to distinguish
     * these records from true lifecycle transitions.
     */
    private void persistEditHistory(Demand demand, String reasonForEdit) {
        DemandStatusHistory history = new DemandStatusHistory();
        history.setDemand(demand);
        history.setFromStatus(demand.getStatus());
        history.setToStatus(demand.getStatus());
        history.setChangedBy(SecurityUtils.getCurrentUserId());
        history.setComments(reasonForEdit);
        demandStatusHistoryRepository.save(history);
    }

    /**
     * Builds a lightweight before/after snapshot of the auditable scalar fields
     * of a demand. Used to populate {@code beforeState}/{@code afterState} in the
     * audit log payload so reviewers can see exactly what changed.
     */
    private Map<String, Object> snapshotDemand(Demand demand) {
        Map<String, Object> snap = new java.util.LinkedHashMap<>();
        snap.put("title",          demand.getTitle());
        snap.put("status",         demand.getStatus() != null ? demand.getStatus().name() : null);
        snap.put("priority",       demand.getPriority() != null ? demand.getPriority().name() : null);
        snap.put("level",          demand.getLevel() != null ? demand.getLevel().name() : null);
        snap.put("employmentType", demand.getEmploymentType() != null ? demand.getEmploymentType().name() : null);
        snap.put("workMode",       demand.getWorkMode() != null ? demand.getWorkMode().name() : null);
        snap.put("location",       demand.getLocation());
        snap.put("businessUnit",   demand.getBusinessUnit());
        snap.put("department",     demand.getDepartment());
        snap.put("accountId",      demand.getAccountId());
        snap.put("accountName",    demand.getAccountName());
        snap.put("projectId",      demand.getProjectId());
        snap.put("projectName",    demand.getProjectName());
        snap.put("budget",         demand.getBudget());
        snap.put("reqUtilPerc",    demand.getReqUtilPerc());
        snap.put("experience",     demand.getExperience());
        snap.put("targetDate",     demand.getTargetDate() != null ? demand.getTargetDate().toString() : null);
        snap.put("searchStartAt",  demand.getSearchStartAt() != null ? demand.getSearchStartAt().toString() : null);
        snap.put("onboardingDate", demand.getOnboardingDate() != null ? demand.getOnboardingDate().toString() : null);
        return snap;
    }

    /**
     * Replaces skill associations via the parent {@link Demand} collection.
     *
     * <p>Existing rows are removed through {@code orphanRemoval} on {@code demandSkills}
     * (not a bulk repository delete), so Hibernate's persistence context stays consistent
     * with the database. New associations are persisted by {@code cascade = ALL} when
     * the demand is saved — a separate {@code saveAll} is not used, avoiding duplicate
     * INSERTs for the same {@code (demand_id, skill_id)}.
     */
    private void syncDemandSkills(Demand demand, List<Long> requestMandatory, List<Long> requestOptional) {
        List<Long> existingMandatory = extractSkillIds(demand.getDemandSkills(), true);
        List<Long> existingOptional = extractSkillIds(demand.getDemandSkills(), false);

        List<Long> mandatoryIds = requestMandatory != null ? requestMandatory : existingMandatory;
        List<Long> optionalIds = requestOptional != null ? requestOptional : existingOptional;

        validationService.validateSkillLists(mandatoryIds, optionalIds);

        List<DemandSkill> newSkills = createDemandSkills(demand, mandatoryIds, optionalIds);
        if (demand.getDemandSkills() == null) {
            demand.setDemandSkills(new ArrayList<>(newSkills));
        } else {
            demand.getDemandSkills().clear();
            demand.getDemandSkills().addAll(newSkills);
        }
    }

    private List<DemandSkill> createDemandSkills(Demand demand, List<Long> mandatoryIds, List<Long> optionalIds) {
        List<DemandSkill> demandSkills = new ArrayList<>();
        Set<Long> assignedSkillIds = new HashSet<>();

        if (mandatoryIds != null && !mandatoryIds.isEmpty()) {
            for (Skill skill : skillLookupService.resolveSkillIds(mandatoryIds)) {
                if (assignedSkillIds.add(skill.getSkillId())) {
                    demandSkills.add(newDemandSkill(demand, skill, true));
                }
            }
        }
        if (optionalIds != null && !optionalIds.isEmpty()) {
            for (Skill skill : skillLookupService.resolveSkillIds(optionalIds)) {
                if (assignedSkillIds.add(skill.getSkillId())) {
                    demandSkills.add(newDemandSkill(demand, skill, false));
                }
            }
        }
        return demandSkills;
    }

    private DemandSkill newDemandSkill(Demand demand, Skill skill, boolean mandatory) {
        DemandSkill ds = new DemandSkill();
        ds.setDemand(demand);
        ds.setSkill(skill);
        ds.setIsMandatory(mandatory);
        return ds;
    }

    private List<Long> extractSkillIds(List<DemandSkill> demandSkills, boolean isMandatory) {
        if (demandSkills == null || demandSkills.isEmpty()) {
            return List.of();
        }
        return demandSkills.stream()
                .filter(ds -> Boolean.TRUE.equals(ds.getIsMandatory()) == isMandatory)
                .map(ds -> ds.getSkill().getSkillId())
                .toList();
    }

    private void enrichDemandFromReferences(Demand demand, DemandRequest request) {
        if (request.getProjectId() != null) {
            try {
                var project = userAuthServiceClient.getProjectById(request.getProjectId());
                if (project != null) {
                    if (project.getName() != null) {
                        demand.setProjectName(project.getName());
                    }
                    if (demand.getAccountId() == null
                            && project.getAccountId() != null
                            && project.getAccountId() > 0) {
                        demand.setAccountId(project.getAccountId());
                    }
                }
            } catch (Exception e) {
                log.warn("Could not fetch project details for id={}: {}", request.getProjectId(), e.getMessage());
            }
        }
        if (demand.getProjectName() == null) {
            demand.setProjectName("Unknown Project");
        }

        Long accountId = demand.getAccountId();
        if (accountId != null) {
            try {
                var account = userAuthServiceClient.getAccountById(accountId);
                if (account != null && account.getName() != null) {
                    demand.setAccountName(account.getName());
                }
            } catch (Exception e) {
                log.warn("Could not fetch account details for id={}: {}", accountId, e.getMessage());
            }
        }
        if (demand.getAccountName() == null) {
            demand.setAccountName("Unknown Account");
        }
        if (demand.getAccountId() == null) {
            throw new IllegalArgumentException(
                    "accountId is required (provide accountId or a projectId linked to an account)");
        }
    }

    private void applyCreateDefaults(Demand demand) {
        if (demand.getEmploymentType() == null) {
            demand.setEmploymentType(EmploymentType.FULL_TIME);
        }
        if (demand.getWorkMode() == null) {
            demand.setWorkMode(WorkMode.REMOTE);
        }
        if (demand.getExperience() == null) {
            demand.setExperience(0L);
        }
        if (demand.getClientInterview() == null) {
            demand.setClientInterview(Boolean.FALSE);
        }
        if (demand.getRequiredCount() == null) {
            demand.setRequiredCount(1);
        }
    }
}