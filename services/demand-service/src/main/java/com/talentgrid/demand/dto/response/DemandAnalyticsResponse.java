package com.talentgrid.demand.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Analytics response for the demand dashboard.
 * Returned by {@code GET /analytics/demands}.
 *
 * <p>Provides aggregate metrics across all non-deleted demands:
 * <ul>
 *   <li>{@code totalDemands}            — total count of demands</li>
 *   <li>{@code draftCount}              — demands in DRAFT status</li>
 *   <li>{@code pendingApprovalCount}    — demands awaiting approval</li>
 *   <li>{@code activeSearchCount}       — demands in INTERNAL_SEARCH or OPEN_EXTERNAL</li>
 *   <li>{@code filledCount}             — demands in FILLED_INTERNAL/EXTERNAL/PARTIALLY</li>
 *   <li>{@code cancelledCount}          — cancelled demands</li>
 *   <li>{@code onHoldCount}             — demands on hold</li>
 *   <li>{@code closedCount}             — closed (terminal) demands</li>
 *   <li>{@code fillRatePercent}         — percentage of demands reaching FILLED states</li>
 *   <li>{@code avgTimeToFillDays}       — average days from creation to filled closure</li>
 *   <li>{@code totalInternalFilled}     — aggregate internal filled positions</li>
 *   <li>{@code totalExternalFilled}     — aggregate external filled positions</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemandAnalyticsResponse {

    private long totalDemands;

    // ── Status breakdown ────────────────────────────────────────────────────────
    private long draftCount;
    private long pendingApprovalCount;
    private long activeSearchCount;
    private long filledCount;
    private long cancelledCount;
    private long onHoldCount;
    private long closedCount;

    // ── Rates ───────────────────────────────────────────────────────────────────
    private double fillRatePercent;
    private double avgTimeToFillDays;

    // ── Fill split (single-person model) ───────────────────────────────────────
    /** Number of demands filled from the internal bench. */
    private long internalFilledCount;

    /** Number of demands filled via external hire. */
    private long externalFilledCount;

}
