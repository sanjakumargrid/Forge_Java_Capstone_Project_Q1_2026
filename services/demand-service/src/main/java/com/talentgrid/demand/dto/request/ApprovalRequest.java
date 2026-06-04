package com.talentgrid.demand.dto.request;

import com.talentgrid.demand.domain.enums.DemandStatus;

/**
 * Request DTO for approving or rejecting a pending demand.
 * Used by {@code POST /demands/{id}/approve}.
 *
 * <p>Valid decision values per spec:
 * <ul>
 *   <li>{@code APPROVED}   — approve the demand (auto-transitions to INTERNAL_SEARCH)</li>
 *   <li>{@code DRAFT}      — reject (return to draft for HM revision)</li>
 *   <li>{@code DUPLICATE}  — mark as duplicate of an existing demand</li>
 *   <li>{@code ON_HOLD}    — place on hold during approval review</li>
 *   <li>{@code CANCELLED}  — cancel outright from pending approval</li>
 * </ul>
 */
public class ApprovalRequest {

    /**
     * The approval decision.  Must be one of the five states reachable from
     * {@code PENDING_APPROVAL}: APPROVED, DRAFT, DUPLICATE, ON_HOLD, CANCELLED.
     */
    private DemandStatus decision;

    /** Optional free-text rationale stored in the status history audit trail. */
    private String comments;

    // ─── Getters & Setters ──────────────────────────────────────────────────────
    public DemandStatus getDecision() { return decision; }
    public void setDecision(DemandStatus decision) { this.decision = decision; }

    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }
}
