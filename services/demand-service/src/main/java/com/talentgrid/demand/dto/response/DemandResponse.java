package com.talentgrid.demand.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Full response DTO for a demand entity.
 * Returned by {@code GET /demands/{id}} and {@code POST /demands}.
 * Maps all 42 fields from the {@code demands} table.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemandResponse {

    private Long demandId;
    private String title;
    private String description;
    private String level;
    private String employmentType;
    private String location;
    private Long accountId;
    private String accountName;
    private Long projectId;
    private String projectName;
    private String businessUnit;
    private Long jobTitleId;
    private List<SkillDto> mandatorySkills;
    private List<SkillDto> optionalSkills;
    private BigDecimal budget;

    /** Required utilization percentage (0–100) for this demand's project/account. */
    private Integer reqUtilPerc;

    /** The designated work mode for the demand (e.g., REMOTE, HYBRID, ONSITE). */
    private String workMode;

    /** Required years of professional experience. */
    private Long experience;

    /** The specific department requesting the demand. */
    private String department;

    /** Indicates whether a client interview is a mandatory step. */
    private Boolean clientInterview;

    /** The target date for the candidate to be officially onboarded. */
    private LocalDate onboardingDate;

    // Fill tracking (single-person model)
    /** Whether this demand has been filled by a matched employee. */
    private Boolean isFilled;

    /**
     * How the demand was filled: {@code "INTERNAL"} or {@code "EXTERNAL"}.
     * {@code null} when the demand is not yet filled.
     */
    private String fillType;

    // Status
    private String status;
    private String priority;
    private String previousStatus;

    // Dates
    private LocalDate targetDate;
    private OffsetDateTime searchStartAt;
    private OffsetDateTime approvedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    // Closure
    private String closureReason;

    // Personnel
    private Long createdBy;
    private String creatorName;
    private String creatorEmail;
    private Long assignedRecruiter;
    private String assignedRecruiterName;
    private Long assignedRm;
    private String assignedRmName;
    private Long approvedBy;
    private String approverName;

    // Flags
    private Boolean isDeleted;
    private Integer version;

}
