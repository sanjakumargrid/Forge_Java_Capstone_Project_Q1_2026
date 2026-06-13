package com.talentgrid.demand.service;

import com.talentgrid.demand.domain.entity.Demand;
import com.talentgrid.demand.domain.enums.DemandStatus;
import com.talentgrid.demand.dto.request.DemandRequest;
import com.talentgrid.demand.dto.response.DemandResponse;
import com.talentgrid.demand.exception.DemandNotFoundException;
import com.talentgrid.demand.exception.InvalidDemandStateException;
import com.talentgrid.demand.mapper.DemandMapper;
import com.talentgrid.demand.repository.DemandRepository;
import com.talentgrid.demand.util.SecurityUtils;
import com.talentgrid.demand.client.UserAuthServiceClient;
import com.talentgrid.audit.client.AuditLogClient;
import com.talentgrid.audit.dto.AuditAction;
import com.talentgrid.audit.dto.AuditLogPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Handles demand CRUD operations (create, update, soft-delete).
 *
 * <p>
 * Business rules enforced:
 * <ul>
 * <li>New demands are always created in {@code DRAFT} status.</li>
 * <li>Updates are only permitted when the demand is in {@code DRAFT}
 * status.</li>
 * <li>Soft-delete (setting {@code is_deleted = true}) is only permitted in
 * {@code DRAFT} status.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DemandService {

    private final DemandRepository demandRepository;
    private final DemandMapper demandMapper;
    private final DemandValidationService validationService;
    private final AuditLogClient auditLogClient;
    private final UserAuthServiceClient userAuthServiceClient;

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

        // Default onboarding date to target date if not provided
        if (demand.getOnboardingDate() == null) {
            demand.setOnboardingDate(demand.getTargetDate());
        }

        // Fetch Account and Project info from UserAuthService
        if (request.getAccountId() != null) {
            try {
                var account = userAuthServiceClient.getAccountById(request.getAccountId());
                if (account != null) demand.setAccountName(account.getName());
            } catch (Exception e) {
                log.warn("Could not fetch account details for id={}: {}", request.getAccountId(), e.getMessage());
                demand.setAccountName("Unknown Account");
            }
        }
        
        if (request.getProjectId() != null) {
            try {
                var project = userAuthServiceClient.getProjectById(request.getProjectId());
                if (project != null) demand.setProjectName(project.getName());
            } catch (Exception e) {
                log.warn("Could not fetch project details for id={}: {}", request.getProjectId(), e.getMessage());
                demand.setProjectName("Unknown Project");
            }
        }

        // Extract the user ID, email, and name from the security context and set it as
        // the creator
        demand.setCreatedBy(SecurityUtils.getCurrentUserId());
        demand.setCreatorEmail(SecurityUtils.getCurrentUserEmail());
        demand.setCreatorName(SecurityUtils.getCurrentUserName());

        Demand saved = demandRepository.save(demand);

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
                .endpoint("/api/demands")
                .build());

        log.info("Demand created successfully with id={}", saved.getDemandId());
        return demandMapper.toResponse(saved);
    }

    /**
     * Updates editable fields of an existing demand.
     * Only permitted when the demand is in {@code DRAFT} status.
     *
     * @param id      the demand ID
     * @param request the partial update request (null fields are ignored)
     * @return the updated demand as a response DTO
     * @throws DemandNotFoundException     if the demand does not exist or is
     *                                     soft-deleted
     * @throws InvalidDemandStateException if the demand is not in {@code DRAFT}
     *                                     status
     */
    @Transactional
    public DemandResponse updateDemand(Long id, DemandRequest request) {
        validationService.validateUpdate(request);
        Demand demand = findActiveOrThrow(id);
        requireDraftState(demand, "update");

        demandMapper.applyUpdate(request, demand);
        Demand saved = demandRepository.save(demand);

        // Publish audit event
        auditLogClient.logAction(AuditLogPayload.builder()
                .entityType("DEMAND")
                .entityId(saved.getDemandId())
                .action(AuditAction.UPDATE)
                .actorId(SecurityUtils.getCurrentUserId())
                .afterState(Map.of(
                        "title", saved.getTitle(),
                        "status", saved.getStatus().name()))
                .serviceName("demand-service")
                .endpoint("/api/demands/" + id)
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
                .endpoint("/api/demands/" + id)
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
     * Asserts the demand is in {@code DRAFT} status. Throws if not.
     */
    private void requireDraftState(Demand demand, String operation) {
        if (demand.getStatus() != DemandStatus.DRAFT) {
            throw new InvalidDemandStateException(
                    String.format("Cannot %s demand (id=%d): current status is %s, expected DRAFT.",
                            operation, demand.getDemandId(), demand.getStatus()));
        }
    }
}
