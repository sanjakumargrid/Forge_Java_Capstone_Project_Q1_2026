package com.talentgrid.demand.dto.request;

import com.talentgrid.demand.domain.enums.DemandPriority;
import com.talentgrid.demand.domain.enums.SeniorityLevel;
import com.talentgrid.demand.domain.enums.EmploymentType;
import com.talentgrid.demand.domain.enums.WorkMode;

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
    private String description;
    private SeniorityLevel level;
    private String location;
    private Long accountId;
    private Long projectId;
    private String businessUnit;
    private List<String> skills;
    private BigDecimal budget;

    /** Required utilization percentage (0–100). See {@code Demand.reqUtilPerc} for semantics. */
    private Integer reqUtilPerc;
    private Integer requiredCount;
    private LocalDate targetDate;
    private DemandPriority priority;
    private EmploymentType employmentType;
    private OffsetDateTime searchStartAt;
    private WorkMode workMode;
    private Long experience;
    private String department;
    private Boolean clientInterview;
    private LocalDate onboardingDate;

}
