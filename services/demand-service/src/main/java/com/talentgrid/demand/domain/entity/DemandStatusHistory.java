package com.talentgrid.demand.domain.entity;

import com.talentgrid.demand.domain.enums.DemandStatus;
import jakarta.persistence.*;

import java.time.OffsetDateTime;

/**
 * Audit trail entity capturing every demand status transition.
 * One record is written per transition, providing a full lifecycle history.
 */
@Entity
@Table(name = "demand_status_history")
public class DemandStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demand_id", nullable = false)
    private Demand demand;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status")
    private DemandStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false)
    private DemandStatus toStatus;

    /** User ID (employee_id) of the actor who performed the transition. */
    @Column(name = "changed_by")
    private Long changedBy;

    /** Optional closure/transition reason code stored as string. */
    @Column(name = "closure_reason", length = 255)
    private String closureReason;

    @Column(name = "comments", columnDefinition = "TEXT")
    private String comments;

    @Column(name = "changed_at", nullable = false)
    private OffsetDateTime changedAt;

    @PrePersist
    protected void prePersist() {
        if (this.changedAt == null) {
            this.changedAt = OffsetDateTime.now();
        }
    }

    // ─── Getters & Setters ──────────────────────────────────────────────────────
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Demand getDemand() { return demand; }
    public void setDemand(Demand demand) { this.demand = demand; }

    public DemandStatus getFromStatus() { return fromStatus; }
    public void setFromStatus(DemandStatus fromStatus) { this.fromStatus = fromStatus; }

    public DemandStatus getToStatus() { return toStatus; }
    public void setToStatus(DemandStatus toStatus) { this.toStatus = toStatus; }

    public Long getChangedBy() { return changedBy; }
    public void setChangedBy(Long changedBy) { this.changedBy = changedBy; }

    public String getClosureReason() { return closureReason; }
    public void setClosureReason(String closureReason) { this.closureReason = closureReason; }

    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }

    public OffsetDateTime getChangedAt() { return changedAt; }
    public void setChangedAt(OffsetDateTime changedAt) { this.changedAt = changedAt; }
}
