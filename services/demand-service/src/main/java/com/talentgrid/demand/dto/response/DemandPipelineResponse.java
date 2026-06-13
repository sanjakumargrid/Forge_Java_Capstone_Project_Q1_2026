package com.talentgrid.demand.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Unified pipeline response combining internal and external candidate views
 * along with the full status history audit trail.
 * Returned by {@code GET /demands/{id}/pipeline}.
 *
 * <p>Aggregates matched candidates from both the internal bench search and
 * external candidate sourcing into a single response for recruiter use.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemandPipelineResponse {

    private Long demandId;
    private String title;
    private String status;

    /** Total headcount required for this demand. */
    private Integer requiredCount;

    /** Positions already filled from internal bench. */
    private Integer internalFilledCount;

    /** Positions already filled from external candidates. */
    private Integer externalFilledCount;

    /** Total recruited count (internal + external). */
    private Integer recruitedCount;

    /** Remaining open positions (requiredCount - recruitedCount). */
    private Integer remainingCount;

    /** Internal candidate matches from the bench (employee IDs or lightweight projections). */
    private List<Long> internalCandidateIds;

    /** External candidate matches (applicant IDs or lightweight projections). */
    private List<Long> externalCandidateIds;

    /** Full status transition audit trail for this demand. */
    private List<DemandStatusHistoryResponse> statusHistory;

}
