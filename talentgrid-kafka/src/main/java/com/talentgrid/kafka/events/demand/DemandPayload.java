package com.talentgrid.kafka.events.demand;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DemandPayload {

    private Long demandId;
    private String title;
    private String status;
    private String level;
    private List<String> skills;
    private String location;
    private String accountName;
    private String projectName;
    private String businessUnit;
    private String priority;
    private Long createdBy;
    private String creatorName;
    private String recipientEmail;
    private String recipientSlackId;
    private String raisedBy;
    private BigDecimal budget;
    private LocalDate targetDate;
    private String description;
    private String workMode;
    private Long experience;
    private String department;
    private String employmentType;
    private LocalDate onboardingDate;
    private Long approvedBy;
    private String approverName;
    private OffsetDateTime approvedAt;
    private Long assignedRm;
    private String assignedRmName;
    private OffsetDateTime searchStartAt;
    private Long assignedRecruiter;
    private String assignedRecruiterName;
    private Integer requiredCount;
    private Integer internalFilledCount;
    private Integer externalFilledCount;
    private Integer recruitedCount;
    private String closureReason;
}
