package com.talentgrid.jobposting.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Mirrors the demand producer's wire format 1:1 (which is also the shape the
 * Angular `demands` app's `Demand` model expects on the way out — see
 * DemandResponse). Unknown fields are ignored rather than erroring, so the
 * envelope can evolve without breaking this consumer.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DemandPayload {

    private Long demandId;
    private String title;
    private String description;
    private String level;
    private String location;
    private String employmentType;
    private Long accountId;
    private Long projectId;
    private String businessUnit;
    private List<String> skills;
    private BigDecimal budget;
    private Integer requiredCount;
    private Integer recruitedCount;
    private Integer internalFilledCount;
    private Integer externalFilledCount;
    private String status;
    private String priority;
    private String previousStatus;
    private Instant targetDate;
    private Instant searchStartAt;
    private Instant approvedAt;
    private Instant createdAt;
    private Instant updatedAt;
    private String closureReason;
    private Long createdBy;
    private Long assignedRecruiter;
    private Long assignedRm;
    private Long approvedBy;
    private Boolean isDeleted;
    private Integer version;
}
