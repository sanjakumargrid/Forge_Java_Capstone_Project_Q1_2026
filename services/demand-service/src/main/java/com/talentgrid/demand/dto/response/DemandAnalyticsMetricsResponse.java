package com.talentgrid.demand.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Position-level analytics response for demand fill rate, time-to-fill, and internal vs external split.
 * Returned by {@code GET /api/v1/analytics/demands} with optional date range filters (default: last 30 days).
 *
 * <p>Metrics:
 * <ul>
 *   <li>Fill Rate: percentage of positions filled (internal + external) / total required positions</li>
 *   <li>Time-to-Fill: average days from demand creation to first FILLED_* status transition</li>
 *   <li>Internal vs External Split: distribution of filled positions by source</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemandAnalyticsMetricsResponse {

    // ── Metadata ────────────────────────────────────────────────────────────────────
    @JsonProperty("metadata")
    private Metadata metadata;

    // ── Fill Rate (Position-Level) ──────────────────────────────────────────────────
    @JsonProperty("fillRate")
    private FillRate fillRate;

    // ── Internal vs External Split ──────────────────────────────────────────────────
    @JsonProperty("internalVsExternalSplit")
    private InternalExternalSplit internalVsExternalSplit;

    // ── Average Time-to-Fill ───────────────────────────────────────────────────────
    @JsonProperty("timeToFill")
    private TimeToFill timeToFill;

    // ── Nested DTOs ─────────────────────────────────────────────────────────────────

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Metadata {
        private LocalDate startDate;
        private LocalDate endDate;
        private Integer windowDays;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FillRate {
        /** Total required positions across all demands in the time window */
        private long totalRequiredPositions;

        /** Total positions filled (internal + external) */
        private long totalFilledPositions;

        /** Fill rate as percentage (0-100) */
        private double fillRatePercent;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InternalExternalSplit {
        /** Count of positions filled from internal bench */
        private long internalFilledCount;

        /** Count of positions filled from external candidates */
        private long externalFilledCount;

        /** Percentage of positions filled internally */
        private double internalPercentage;

        /** Percentage of positions filled externally */
        private double externalPercentage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeToFill {
        /** Average days from demand creation to first FILLED_* status transition */
        private double averageDaysToFill;

        /** Count of demands that were filled in the time window */
        private long totalFilledDemands;

        /** Minimum days to fill (for a demand with filled positions) */
        private double minDaysToFill;

        /** Maximum days to fill (for a demand with filled positions) */
        private double maxDaysToFill;
    }
}

