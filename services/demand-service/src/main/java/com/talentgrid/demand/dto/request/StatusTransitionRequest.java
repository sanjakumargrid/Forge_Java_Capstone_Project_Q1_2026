package com.talentgrid.demand.dto.request;

import com.talentgrid.demand.domain.enums.ClosureReason;
import com.talentgrid.demand.domain.enums.DemandStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for performing a demand workflow state transition.
 * Used by {@code PATCH /demands/{id}/status}.
 *
 * <p>The {@code closureReason} must match the required reason for the target state
 * as enforced by {@code TransitionValidator}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatusTransitionRequest {

    /** The requested target status for this demand. */
    private DemandStatus targetStatus;

    /**
     * Closure or transition reason code.
     * Required for several targets (for example {@code FILLED}, {@code CLOSED}, {@code ON_HOLD})
     * as enforced by {@code TransitionValidator}.
     */
    private ClosureReason closureReason;

    /** Optional free-text comment to record in the status history audit trail. */
    private String comments;

}
