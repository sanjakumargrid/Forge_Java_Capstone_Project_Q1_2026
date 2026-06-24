
package com.talentgrid.jobposting.dto.response;

import com.talentgrid.jobposting.entity.Demand;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Mirrors the frontend `Demand` interface
 * (apps/demands/src/app/state/demands.models.ts) field-for-field — that's
 * the shape the Open Demands table, filters, KPI cards and status changer
 * are all actually built against.
 */
@Data
@Builder
public class DemandResponse {

    private Long demandId;
    private String title;
    private String description;
    private String level;
    private String location;
    private String employmentType;

    /** No account/CRM integration yet — always a placeholder. */
    private String accountName;
    private Long accountId;
    /** No project-catalog integration yet — always a placeholder. */
    private String projectName;
    private Long projectId;

    private String businessUnit;
    private List<String> skills;
    private List<String> mandatorySkills;
    private List<String> optionalSkills;
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
    /** No user-directory integration yet — always a placeholder. */
    private String creatorName;
    private String creatorEmail;

    private Long assignedRecruiter;
    private String assignedRecruiterName;
    private Long assignedRm;
    private String assignedRmName;
    private Long approvedBy;
    private String approverName;

    private Boolean isDeleted;
    private Integer version;

    // ── External-posting specifics (from DEMAND_EXTERNAL_OPENED) ───────────────
    private String workMode;
    private String experience;
    private String department;
    private String onboardingDate;

    // ── Notification & creator routing (from DEMAND_EXTERNAL_OPENED) ───────────
    private String recipientEmail;
    private String recipientSlackId;
    private String raisedBy;

    /** Always 0 — applicant tracking is not yet implemented. */
    private Integer applicantCount;
    /** Always 0 — AI matching is not yet implemented. */
    private Integer aiMatchScore;
    /** Derived from targetDate vs. now — not sourced from the demand system. */
    private String slaStatus;

    public static DemandResponse from(Demand d) {
        return DemandResponse.builder()
                .demandId(d.getDemandId())
                .title(d.getTitle())
                .description(d.getDescription())
                .level(d.getLevel())
                .location(d.getLocation())
                .employmentType(d.getEmploymentType())
                .accountName(d.getAccountName() != null ? d.getAccountName() : "—")
                .accountId(d.getAccountId())
                .projectName(d.getProjectName() != null ? d.getProjectName() : "—")
                .projectId(d.getProjectId())
                .businessUnit(d.getBusinessUnit())
                .skills(d.getSkills())
                .mandatorySkills(d.getMandatorySkills())
                .optionalSkills(d.getOptionalSkills())
                .budget(d.getBudget())
                .requiredCount(d.getRequiredCount())
                .recruitedCount(d.getRecruitedCount() != null ? d.getRecruitedCount() : 0)
                .internalFilledCount(d.getInternalFilledCount() != null ? d.getInternalFilledCount() : 0)
                .externalFilledCount(d.getExternalFilledCount() != null ? d.getExternalFilledCount() : 0)
                .status(d.getStatus())
                .priority(d.getPriority())
                .previousStatus(d.getPreviousStatus())
                .targetDate(d.getTargetDate())
                .searchStartAt(d.getSearchStartAt())
                .approvedAt(d.getApprovedAt())
                .createdAt(d.getSourceCreatedAt())
                .updatedAt(d.getSourceUpdatedAt())
                .closureReason(d.getClosureReason())
                .createdBy(d.getCreatedBy())
                .creatorName(d.getCreatorName() != null ? d.getCreatorName() : "Unassigned")
                .creatorEmail(d.getRecipientEmail() != null ? d.getRecipientEmail() : "")
                .assignedRecruiter(d.getAssignedRecruiter())
                .assignedRecruiterName(d.getAssignedRecruiterName() != null ? d.getAssignedRecruiterName() : "Unassigned")
                .assignedRm(d.getAssignedRm())
                .assignedRmName(d.getAssignedRmName() != null ? d.getAssignedRmName() : "Unassigned")
                .approvedBy(d.getApprovedBy())
                .approverName(d.getApprovedBy() != null ? "Unassigned" : null)
                .isDeleted(Boolean.TRUE.equals(d.getIsDeleted()))
                .version(d.getVersion() != null ? d.getVersion() : 1)
                .workMode(d.getWorkMode())
                .experience(d.getExperience())
                .department(d.getDepartment())
                .onboardingDate(d.getOnboardingDate())
                .recipientEmail(d.getRecipientEmail())
                .recipientSlackId(d.getRecipientSlackId())
                .raisedBy(d.getRaisedBy())
                .applicantCount(0)
                .aiMatchScore(0)
                .slaStatus(computeSlaStatus(d.getTargetDate(), d.getStatus()))
                .build();
    }

    private static String computeSlaStatus(Instant targetDate, String status) {
        if ("CLOSED".equals(status) || "FILLED_INTERNAL".equals(status) || "FILLED_EXTERNAL".equals(status)) {
            return "on_track";
        }
        if (targetDate == null) {
            return "on_track";
        }
        long daysRemaining = Duration.between(Instant.now(), targetDate).toDays();
        if (daysRemaining < 0) return "overdue";
        if (daysRemaining <= 7) return "at_risk";
        return "on_track";
    }
}
