package com.talentgrid.demand.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Response DTO representing a single demand status history entry.
 * Provides the full audit trail of a demand's lifecycle transitions.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

}
