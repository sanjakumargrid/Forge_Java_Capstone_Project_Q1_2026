package com.talentgrid.demand.dto.request;

import com.talentgrid.demand.domain.enums.DemandPriority;
import com.talentgrid.demand.domain.enums.EmploymentType;
import com.talentgrid.demand.domain.enums.WorkMode;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Unified request DTO for creating and updating a workforce demand.
 *
 * <p>Used by:
 * <ul>
 *   <li>{@code POST  /demands}      — all fields are expected (create)</li>
 *   <li>{@code PATCH /demands/{id}} — only non-null fields are applied (update)</li>
 * </ul>
 *
 * <p>Validation rules differ by operation and are enforced in
 * {@code DemandValidationService}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemandRequest {

    private String title;

    @NotBlank(message = "Description is required.")
    @Size(min = 250, max = 2000, message = "Description must be between 250 and 2000 characters.")
    private String description;
    private String level;
    private String location;
    private Long accountId;
    private Long projectId;
    private String businessUnit;
    private Long jobTitleId;
    private List<Long> mandatorySkillIds;
    private List<Long> optionalSkillIds;
    private BigDecimal budget;

    /** Required utilization percentage (0–100). See {@code Demand.reqUtilPerc} for semantics. */
    private Integer reqUtilPerc;
    private LocalDate targetDate;
    private DemandPriority priority;
    private EmploymentType employmentType;
    private OffsetDateTime searchStartAt;
    private WorkMode workMode;
    private Long experience;
    private String department;
    private Boolean clientInterview;
    private LocalDate onboardingDate;

    /** When true, post-approval flow skips internal search (bench hiring). */
    private Boolean benchHiring;

    /**
     * Mandatory reason explaining why this demand is being edited.
     * Required for all PATCH /demands/{id} requests; persisted to edit history for audit.
     */
    private String reasonForEdit;

}

