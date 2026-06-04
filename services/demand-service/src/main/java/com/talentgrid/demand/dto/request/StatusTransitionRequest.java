package com.talentgrid.demand.dto.request;

import com.talentgrid.demand.domain.enums.ClosureReason;
import com.talentgrid.demand.domain.enums.DemandStatus;

/**
 * Request DTO for performing a demand workflow state transition.
 * Used by {@code PATCH /demands/{id}/status}.
 *
 * <p>The {@code closureReason} must match the required reason for the target state
 * as enforced by {@code TransitionValidator}.
 */
public class StatusTransitionRequest {

    /** The requested target status for this demand. */
    private DemandStatus targetStatus;

    /**
     * Closure or transition reason code.
     * Required when transitioning to: {@code FILLED_INTERNAL}, {@code FILLED_EXTERNAL},
     * {@code CANCELLED}, {@code ON_HOLD}, {@code DUPLICATE}.
     */
    private ClosureReason closureReason;

    /** Optional free-text comment to record in the status history audit trail. */
    private String comments;

    // ─── Getters & Setters ──────────────────────────────────────────────────────
    public DemandStatus getTargetStatus() { return targetStatus; }
    public void setTargetStatus(DemandStatus targetStatus) { this.targetStatus = targetStatus; }

    public ClosureReason getClosureReason() { return closureReason; }
    public void setClosureReason(ClosureReason closureReason) { this.closureReason = closureReason; }

    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }
}
