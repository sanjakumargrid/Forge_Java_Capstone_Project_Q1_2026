package com.talentgrid.workforce.rmgdashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StatusUpdateRequest {
    private String status;

    /**
     * Closure reason code — required by demand-service when transitioning to
     * FILLED_INTERNAL, FILLED_EXTERNAL, CANCELLED, ON_HOLD, or DUPLICATE.
     * Must match a valid ClosureReason enum value on the demand-service side.
     */
    private String closureReason;

    /** Optional free-text comment recorded in the demand status-history audit trail. */
    private String comments;
}
