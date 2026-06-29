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
    /**
     * Target workflow status. Accepts demand-service statuses (e.g. OPEN_EXTERNAL, FILLED, CLOSED)
     * and RMG aliases (FILLED_INTERNAL, FILLED_EXTERNAL, CANCELLED, DUPLICATE).
     */
    private String status;

    /**
     * Closure reason — required for ON_HOLD and CLOSED; required for OPEN_EXTERNAL when bypassing
     * the internal-search gate. Use NO_INTERNAL_MATCH or HM_REJECTED_NOMINATION for OPEN_EXTERNAL.
     */
    private String closureReason;

    /** Optional free-text comment recorded in the demand status-history audit trail. */
    private String comments;
}
