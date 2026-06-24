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
 *
 * <p>The richer {@code DEMAND_EXTERNAL_OPENED} event carries denormalised,
 * human-readable fields (account/project names, recruiter/RM names, creator
 * name) and external-posting specifics (work mode, experience, department,
 * employment type, onboarding date) plus notification routing
 * (recipient email/Slack id, raisedBy). These are captured here so the
 * consumer can persist real values instead of placeholders.
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

    /**
     * Legacy combined skill list. Newer events split skills into
     * {@link #mandatorySkills} / {@link #optionalSkills}; {@code getEffectiveSkills()}
     * coalesces both shapes for downstream persistence.
     */
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
    private Long assignedRecruiter;
    private Long assignedRm;
    private Long approvedBy;
    private Boolean isDeleted;
    private Integer version;

    // ── Denormalised display names (DEMAND_EXTERNAL_OPENED) ────────────────────
    private String accountName;
    private String projectName;
    private String creatorName;
    private String assignedRecruiterName;
    private String assignedRmName;

    // ── External-posting specifics (DEMAND_EXTERNAL_OPENED) ────────────────────
    private String workMode;
    private String experience;
    private String department;
    private String onboardingDate;

    // ── Notification & creator routing (DEMAND_EXTERNAL_OPENED) ────────────────
    private String recipientEmail;
    private String recipientSlackId;
    private String raisedBy;

    /**
     * Returns the skill list to persist, preferring the explicit mandatory +
     * optional split (mandatory first, then optional) when present, and falling
     * back to the legacy flat {@code skills} list otherwise.
     */
    public List<String> getEffectiveSkills() {
        boolean hasSplit =
                (mandatorySkills != null && !mandatorySkills.isEmpty())
                        || (optionalSkills != null && !optionalSkills.isEmpty());
        if (!hasSplit) {
            return skills;
        }
        java.util.List<String> combined = new java.util.ArrayList<>();
        if (mandatorySkills != null) combined.addAll(mandatorySkills);
        if (optionalSkills != null) combined.addAll(optionalSkills);
        return combined;
    }
}
