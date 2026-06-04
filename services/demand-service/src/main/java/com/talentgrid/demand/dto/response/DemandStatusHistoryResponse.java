package com.talentgrid.demand.dto.response;

import java.time.OffsetDateTime;

/**
 * Response DTO representing a single demand status history entry.
 * Provides the full audit trail of a demand's lifecycle transitions.
 */
public class DemandStatusHistoryResponse {

    private Long id;
    private Long demandId;
    private String fromStatus;
    private String toStatus;

    /** User ID (employee_id) of the actor who performed the transition. */
    private Long changedBy;

    /** Closure or transition reason code, if applicable. */
    private String closureReason;

    private String comments;
    private OffsetDateTime changedAt;

    // ─── Getters & Setters ──────────────────────────────────────────────────────
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getDemandId() { return demandId; }
    public void setDemandId(Long demandId) { this.demandId = demandId; }

    public String getFromStatus() { return fromStatus; }
    public void setFromStatus(String fromStatus) { this.fromStatus = fromStatus; }

    public String getToStatus() { return toStatus; }
    public void setToStatus(String toStatus) { this.toStatus = toStatus; }

    public Long getChangedBy() { return changedBy; }
    public void setChangedBy(Long changedBy) { this.changedBy = changedBy; }

    public String getClosureReason() { return closureReason; }
    public void setClosureReason(String closureReason) { this.closureReason = closureReason; }

    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }

    public OffsetDateTime getChangedAt() { return changedAt; }
    public void setChangedAt(OffsetDateTime changedAt) { this.changedAt = changedAt; }
}
