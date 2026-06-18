package com.talentgrid.workforce.rmgdashboard.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DemandSummaryDto {
    private Long demandId;
    private String title;
    private String description;
    private String level;
    private String location;
    private Long projectId;
    private String projectName;
    private Long accountId;
    private String accountName;
    private String businessUnit;
    private List<String> skills;
    private BigDecimal budget;
    private String employmentType;
    private String status;
    private String priority;
    private String previousStatus;
    private LocalDate targetDate;
    private OffsetDateTime searchStartAt;
    private OffsetDateTime approvedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private String closureReason;
    private Long createdBy;
    private String creatorName;
    private String creatorEmail;
    private Long assignedRecruiter;
    private String assignedRecruiterName;
    private Long assignedRm;
    private String assignedRmName;
    private Long approvedBy;
    private String approverName;
    private Long ageInDays;
    private Integer internalFilledCount;
    private Integer externalFilledCount;
    private Integer recruitedCount;
    private Integer requiredCount;
    private Boolean isDeleted;
    private Integer version;
}
