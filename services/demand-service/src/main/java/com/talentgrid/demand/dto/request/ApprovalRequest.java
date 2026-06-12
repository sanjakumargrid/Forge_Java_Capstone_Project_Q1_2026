package com.talentgrid.demand.dto.request;

import com.talentgrid.demand.domain.enums.DemandStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalRequest {

    /**
     * The approval decision.  Must be one of the five states reachable from
     * {@code PENDING_APPROVAL}: APPROVED, DRAFT, DUPLICATE, ON_HOLD, CANCELLED.
     */
    private DemandStatus decision;

    /** Optional free-text rationale stored in the status history audit trail. */
    private String comments;

    /** Recruiter assigned during approval. */
    private Long assignedRecruiter;
    private String assignedRecruiterName;

    /** RM assigned during approval. */
    private Long assignedRm;
    private String assignedRmName;

}
