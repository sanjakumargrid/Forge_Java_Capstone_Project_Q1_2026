package com.talentgrid.demand.mapper;

import com.talentgrid.demand.domain.entity.Demand;
import com.talentgrid.demand.domain.entity.DemandStatusHistory;
import com.talentgrid.demand.domain.enums.DemandPriority;
import com.talentgrid.demand.domain.enums.DemandStatus;
import com.talentgrid.demand.domain.enums.SeniorityLevel;
import com.talentgrid.demand.dto.request.CreateDemandRequest;
import com.talentgrid.demand.dto.request.UpdateDemandRequest;
import com.talentgrid.demand.dto.response.DemandResponse;
import com.talentgrid.demand.dto.response.DemandStatusHistoryResponse;
import com.talentgrid.demand.dto.response.DemandSummaryResponse;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Maps between {@link Demand} / {@link DemandStatusHistory} entities and
 * their corresponding request/response DTOs.
 *
 * <p>All enum-to-string conversions use {@code .name()} to ensure the JSON
 * representation matches the enum constant name exactly.
 */
@Component
public class DemandMapper {

    // ─── CreateDemandRequest → Demand ───────────────────────────────────────────

    /**
     * Creates a new {@link Demand} entity from a create request.
     * Sets the initial status to {@code DRAFT} and zeroes all fill counts.
     *
     * @param request the create request (must not be {@code null})
     * @return a new, unpersisted {@link Demand} entity
     */
    public Demand toEntity(CreateDemandRequest request) {
        Demand demand = new Demand();
        demand.setTitle(request.getTitle());
        demand.setDescription(request.getDescription());
        demand.setLevel(request.getLevel());
        demand.setLocation(request.getLocation());
        demand.setProjectId(request.getProjectId());
        demand.setBusinessUnit(request.getBusinessUnit());
        demand.setSkills(request.getSkills());
        demand.setBudget(request.getBudget());
        demand.setRequiredCount(request.getRequiredCount());
        demand.setTargetDate(request.getTargetDate());
        demand.setPriority(request.getPriority());
        // Defaults set by business rule
        demand.setStatus(DemandStatus.DRAFT);
        demand.setInternalFilledCount(0);
        demand.setExternalFilledCount(0);
        demand.setRecruitedCount(0);
        demand.setIsDeleted(false);
        return demand;
    }

    // ─── UpdateDemandRequest → Demand (partial patch) ───────────────────────────

    /**
     * Applies non-null fields from an {@link UpdateDemandRequest} onto an existing
     * {@link Demand} entity. Null fields are ignored (PATCH semantics).
     *
     * @param request the update request
     * @param demand  the existing entity to patch
     */
    public void applyUpdate(UpdateDemandRequest request, Demand demand) {
        if (request.getTitle() != null)        demand.setTitle(request.getTitle());
        if (request.getDescription() != null)  demand.setDescription(request.getDescription());
        if (request.getLevel() != null)        demand.setLevel(request.getLevel());
        if (request.getLocation() != null)     demand.setLocation(request.getLocation());
        if (request.getProjectId() != null)    demand.setProjectId(request.getProjectId());
        if (request.getBusinessUnit() != null) demand.setBusinessUnit(request.getBusinessUnit());
        if (request.getSkills() != null)       demand.setSkills(request.getSkills());
        if (request.getBudget() != null)       demand.setBudget(request.getBudget());
        if (request.getRequiredCount() != null) demand.setRequiredCount(request.getRequiredCount());
        if (request.getTargetDate() != null)   demand.setTargetDate(request.getTargetDate());
        if (request.getPriority() != null)     demand.setPriority(request.getPriority());
    }

    // ─── Demand → DemandResponse ────────────────────────────────────────────────

    /**
     * Maps a {@link Demand} entity to the full {@link DemandResponse} DTO.
     *
     * @param demand the entity to map (returns {@code null} if input is {@code null})
     * @return the full response DTO
     */
    public DemandResponse toResponse(Demand demand) {
        if (demand == null) return null;

        DemandResponse response = new DemandResponse();
        response.setDemandId(demand.getDemandId());
        response.setTitle(demand.getTitle());
        response.setDescription(demand.getDescription());
        response.setLevel(enumName(demand.getLevel()));
        response.setLocation(demand.getLocation());
        response.setProjectId(demand.getProjectId());
        response.setBusinessUnit(demand.getBusinessUnit());
        response.setSkills(demand.getSkills());
        response.setBudget(demand.getBudget());
        response.setRequiredCount(demand.getRequiredCount());
        response.setRecruitedCount(demand.getRecruitedCount());
        response.setInternalFilledCount(demand.getInternalFilledCount());
        response.setExternalFilledCount(demand.getExternalFilledCount());
        response.setStatus(enumName(demand.getStatus()));
        response.setPriority(enumName(demand.getPriority()));
        response.setPreviousStatus(enumName(demand.getPreviousStatus()));
        response.setTargetDate(demand.getTargetDate());
        response.setSearchStartAt(demand.getSearchStartAt());
        response.setApprovedAt(demand.getApprovedAt());
        response.setClosureReason(demand.getClosureReason());
        response.setCreatedBy(demand.getCreatedBy());
        response.setAssignedRecruiter(demand.getAssignedRecruiter());
        response.setAssignedRm(demand.getAssignedRm());
        response.setApprovedBy(demand.getApprovedBy());
        response.setIsDeleted(demand.getIsDeleted());
        response.setVersion(demand.getVersion());
        response.setCreatedAt(demand.getCreatedAt());
        response.setUpdatedAt(demand.getUpdatedAt());
        return response;
    }

    // ─── Demand → DemandSummaryResponse ─────────────────────────────────────────

    /**
     * Maps a {@link Demand} entity to the lightweight {@link DemandSummaryResponse}
     * projection used by the dashboard and list endpoints.
     *
     * <p>Computes {@code ageInDays} as the number of days since {@code createdAt}.
     *
     * @param demand the entity to map (returns {@code null} if input is {@code null})
     * @return the summary response DTO
     */
    public DemandSummaryResponse toSummaryResponse(Demand demand) {
        if (demand == null) return null;

        DemandSummaryResponse response = new DemandSummaryResponse();
        response.setDemandId(demand.getDemandId());
        response.setTitle(demand.getTitle());
        response.setStatus(enumName(demand.getStatus()));
        response.setPriority(enumName(demand.getPriority()));
        response.setBusinessUnit(demand.getBusinessUnit());
        response.setInternalFilledCount(demand.getInternalFilledCount());
        response.setExternalFilledCount(demand.getExternalFilledCount());
        response.setRequiredCount(demand.getRequiredCount());
        response.setCreatedAt(demand.getCreatedAt());

        // Compute age in days
        if (demand.getCreatedAt() != null) {
            long days = ChronoUnit.DAYS.between(demand.getCreatedAt(), OffsetDateTime.now());
            response.setAgeInDays(days);
        }

        return response;
    }

    /**
     * Maps a list of {@link Demand} entities to summary responses.
     *
     * @param demands list of entities (may be empty, must not be {@code null})
     * @return list of summary DTOs
     */
    public List<DemandSummaryResponse> toSummaryResponseList(List<Demand> demands) {
        return demands.stream()
                .map(this::toSummaryResponse)
                .collect(Collectors.toList());
    }

    // ─── DemandStatusHistory → DemandStatusHistoryResponse ──────────────────────

    /**
     * Maps a {@link DemandStatusHistory} audit entity to its response DTO.
     *
     * @param history the audit entity (returns {@code null} if input is {@code null})
     * @return the history response DTO
     */
    public DemandStatusHistoryResponse toHistoryResponse(DemandStatusHistory history) {
        if (history == null) return null;

        DemandStatusHistoryResponse response = new DemandStatusHistoryResponse();
        response.setId(history.getId());
        response.setDemandId(
                history.getDemand() != null ? history.getDemand().getDemandId() : null);
        response.setFromStatus(enumName(history.getFromStatus()));
        response.setToStatus(enumName(history.getToStatus()));
        response.setChangedBy(history.getChangedBy());
        response.setClosureReason(history.getClosureReason());
        response.setComments(history.getComments());
        response.setChangedAt(history.getChangedAt());
        return response;
    }

    /**
     * Maps a list of {@link DemandStatusHistory} entities to response DTOs.
     */
    public List<DemandStatusHistoryResponse> toHistoryResponseList(List<DemandStatusHistory> histories) {
        return histories.stream()
                .map(this::toHistoryResponse)
                .collect(Collectors.toList());
    }

    // ─── Private helpers ─────────────────────────────────────────────────────────

    /** Returns {@code null}-safe {@code .name()} on any enum. */
    private <E extends Enum<E>> String enumName(E value) {
        return value != null ? value.name() : null;
    }
}
