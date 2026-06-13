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
 * Maps all 37 fields from the {@code demands} table.
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
    private List<String> skills;
    private BigDecimal budget;

    /** Required utilization percentage (0–100) for this demand's project/account. */
    private Integer reqUtilPerc;

    // Headcount
    private Integer requiredCount;
    private Integer recruitedCount;
    private Integer internalFilledCount;
    private Integer externalFilledCount;

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
