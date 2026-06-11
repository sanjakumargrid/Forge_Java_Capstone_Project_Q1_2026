package com.talentgrid.workforce.rmgnomination.entity;

import com.talentgrid.workforce.engineerprofilemanagement.entity.InternalEmployee;
import com.talentgrid.workforce.rmgnomination.enums.MatchStatus;
import com.talentgrid.workforce.rmgnomination.enums.NominationType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "internal_matches",
        indexes = {
                @Index(name = "idx_im_employee_id",  columnList = "employee_id"),
                @Index(name = "idx_im_demand_id",    columnList = "demand_id"),
                @Index(name = "idx_im_match_status", columnList = "match_status"),
                @Index(name = "idx_im_is_deleted",   columnList = "is_deleted")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternalMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private InternalEmployee employee;

    @Column(name = "demand_id", nullable = false)
    private Long demandId;

    @Column(name = "nominated_by", nullable = false)
    private Long nominatedBy;

    @Column(name = "nominated_at", nullable = false, updatable = false)
    private LocalDateTime nominatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_status", nullable = false, length = 20)
    @Builder.Default
    private MatchStatus matchStatus = MatchStatus.PENDING_REVIEW;

    @Column(name = "match_score", precision = 5, scale = 2)
    private BigDecimal matchScore;

    @Column(name = "fit_percentage")
    private Integer fitPercentage;

    @Column(name = "missing_skills", columnDefinition = "text[]")
    private String[] missingSkills;

    @Column(name = "match_rationale", columnDefinition = "text")
    private String matchRationale;

    @Column(name = "nomination_reason_by_rmg", columnDefinition = "text")
    private String nominationReasonByRmg;

    @Enumerated(EnumType.STRING)
    @Column(name = "nomination_type", nullable = false, length = 20)
    @Builder.Default
    private NominationType nominationType = NominationType.MANUAL;

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "approval_reason_by_hm", columnDefinition = "text")
    private String approvalReasonByHm;

    @Column(name = "rejection_reason_hm")
    private String rejectionReasonHm;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt    = now;
        this.updatedAt    = now;
        this.nominatedAt  = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
