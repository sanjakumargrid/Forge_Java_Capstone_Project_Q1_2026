package com.talentgrid.jobposting.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * A hiring demand ingested from the demand-intake Kafka topic. Field names
 * intentionally mirror the Angular `demands` app's `Demand` model
 * (apps/demands/src/app/state/demands.models.ts) so DemandResponse can pass
 * values straight through without renaming.
 */
@Entity
@Table(name = "demands")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Demand {

    @Id
    @Column(name = "demand_id")
    private Long demandId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String level;
    private String location;

    @Column(name = "employment_type")
    private String employmentType;

    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "business_unit")
    private String businessUnit;

    @ElementCollection
    @CollectionTable(name = "demand_skills", joinColumns = @JoinColumn(name = "demand_id"))
    @Column(name = "skill")
    private List<String> skills;

    @ElementCollection
    @CollectionTable(name = "demand_mandatory_skills", joinColumns = @JoinColumn(name = "demand_id"))
    @Column(name = "skill")
    private List<String> mandatorySkills;

    @ElementCollection
    @CollectionTable(name = "demand_optional_skills", joinColumns = @JoinColumn(name = "demand_id"))
    @Column(name = "skill")
    private List<String> optionalSkills;

    private BigDecimal budget;

    @Column(name = "required_count")
    private Integer requiredCount;

    @Column(name = "recruited_count")
    private Integer recruitedCount;

    @Column(name = "internal_filled_count")
    private Integer internalFilledCount;

    @Column(name = "external_filled_count")
    private Integer externalFilledCount;

    /** Raw lifecycle status string from the source system (e.g. OPEN_EXTERNAL). */
    private String status;

    private String priority;

    @Column(name = "previous_status")
    private String previousStatus;

    private LocalDate targetDate;

    @Column(name = "search_start_at")
    private OffsetDateTime searchStartAt;

    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;

    /** "createdAt" as reported by the source demand system — distinct from receivedAt below. */@Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
    @Column(name = "closure_reason")
    private String closureReason;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "assigned_recruiter")
    private Long assignedRecruiter;

    @Column(name = "assigned_rm")
    private Long assignedRm;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "is_deleted")
    private Boolean isDeleted;

    @Version
    @Column(name = "version")
    private Integer version;
    // ── Denormalised display names (from DEMAND_EXTERNAL_OPENED) ───────────────

    @Column(name = "account_name")
    private String accountName;

    @Column(name = "project_name")
    private String projectName;

    @Column(name = "creator_name")
    private String creatorName;

    @Column(name = "assigned_recruiter_name")
    private String assignedRecruiterName;

    @Column(name = "assigned_rm_name")
    private String assignedRmName;

    // ── External-posting specifics (from DEMAND_EXTERNAL_OPENED) ───────────────

    @Column(name = "work_mode")
    private String workMode;

    private Long experience;

    private String department;

    private LocalDate onboardingDate;
    // ── Notification & creator routing (from DEMAND_EXTERNAL_OPENED) ───────────

    @Column(name = "recipient_email")
    private String recipientEmail;

    @Column(name = "recipient_slack_id")
    private String recipientSlackId;

    @Column(name = "raised_by")
    private String raisedBy;

    @Column(name = "event_id")
    private String eventId;

    @Column(name = "correlation_id")
    private String correlationId;

    @Column(name = "job_title_id")
    private Long jobTitleId;

    @Column(name = "req_util_perc")
    private Integer reqUtilPerc;

    @Column(name = "client_interview")
    private Boolean clientInterview;

    @Column(name = "bench_hiring")
    private Boolean benchHiring;

    @Column(name = "fill_type")
    private String fillType;

    @Column(name = "approval_reminder_sent")
    private Boolean approvalReminderSent;

    @Column(name = "creator_email")
    private String creatorEmail;

    @Column(name = "creator_slack_id")
    private String creatorSlackId;

    @Column(name = "approver_name")
    private String approverName;

    /** When *our* system first ingested this demand — distinct from sourceCreatedAt. */
    @Column(name = "received_at", nullable = false, updatable = false)
    private LocalDateTime receivedAt;

    @PrePersist
    protected void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();

        if (createdAt == null) {
            createdAt = now;
        }

        updatedAt = now;

        if (receivedAt == null) {
            receivedAt = LocalDateTime.now();
        }

        if (isDeleted == null) {
            isDeleted = false;
        }

        if (benchHiring == null) {
            benchHiring = false;
        }

        if (approvalReminderSent == null) {
            approvalReminderSent = false;
        }

        if (requiredCount == null) {
            requiredCount = 1;
        }
    }

    @PreUpdate
    protected void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
