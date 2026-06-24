package com.talentgrid.jobposting.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "demand_id", unique = true, nullable = false)
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

    @Column(name = "target_date")
    private Instant targetDate;

    @Column(name = "search_start_at")
    private Instant searchStartAt;

    @Column(name = "approved_at")
    private Instant approvedAt;

    /** "createdAt" as reported by the source demand system — distinct from receivedAt below. */
    @Column(name = "source_created_at")
    private Instant sourceCreatedAt;

    @Column(name = "source_updated_at")
    private Instant sourceUpdatedAt;

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

    private String experience;

    private String department;

    @Column(name = "onboarding_date")
    private String onboardingDate;

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

    /** When *our* system first ingested this demand — distinct from sourceCreatedAt. */
    @Column(name = "received_at", nullable = false, updatable = false)
    private LocalDateTime receivedAt;

    @PrePersist
    public void prePersist() {
        this.receivedAt = LocalDateTime.now();
        if (this.isDeleted == null) {
            this.isDeleted = false;
        }
    }
}
