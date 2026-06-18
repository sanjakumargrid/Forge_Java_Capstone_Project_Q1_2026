package com.talentgrid.workforce.skillgapheatmap.provider.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Mirrors the fields from demand-service's DemandResponse
 * (GET /api/v1/demands/{id}) that are relevant to the skill gap heatmap.
 * Kept as a local DTO so the workforce-service has no compile-time
 * dependency on the demand-service module.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DemandServiceResponse {

    private Long demandId;
    private String title;
    private String description;
    private String level;
    private String location;
    private Long projectId;
    private String businessUnit;
    private BigDecimal budget;
    private Integer recruitedCount;
    private Integer internalFilledCount;
    private Integer externalFilledCount;
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
    private Long assignedRecruiter;
    private Long assignedRm;
    private Long approvedBy;
    private Boolean isDeleted;
    private Integer version;
    private Integer requiredCount;
    private List<String> skills;
}
