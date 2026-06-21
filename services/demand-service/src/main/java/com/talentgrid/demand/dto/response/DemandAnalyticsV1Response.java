package com.talentgrid.demand.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * V1 Analytics API response for demand metrics and capacity analysis.
 * Consumed by dashboard team; read-only aggregate data.
 *
 * <p>Metrics returned:
 * <ul>
 *   <li>fillRate: % of filled demands (FILLED_INTERNAL, FILLED_EXTERNAL, FILLED_PARTIALLY) / total non-cancelled</li>
 *   <li>avgTimeToFillDays: average days from creation to first FILLED_* status transition</li>
 *   <li>internalVsExternalSplit: counts and % breakdown by closure reason</li>
 *   <li>capacityByProjectClient: demand counts grouped by projectId + clientId (account)</li>
 * </ul>
 *
 * <p>Filters applied (optional query params):
 * <ul>
 *   <li>dateFrom, dateTo: filter by demand creation date</li>
 *   <li>businessUnit: filter by business unit</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemandAnalyticsV1Response {

    @JsonProperty("metadata")
    private Metadata metadata;

    @JsonProperty("fillRate")
    private FillRate fillRate;

    @JsonProperty("avgTimeToFillDays")
    private Double avgTimeToFillDays;

    @JsonProperty("internalVsExternalSplit")
    private InternalVsExternalSplit internalVsExternalSplit;

    @JsonProperty("capacityByProjectClient")
    private List<CapacityByProjectClient> capacityByProjectClient;

    // ── Nested DTOs ──────────────────────────────────────────────────────────

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Metadata {
        private LocalDate dateFrom;
        private LocalDate dateTo;
        private String businessUnit;
        private Integer windowDays;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FillRate {
        /** Total non-cancelled demands (excludes CANCELLED, DUPLICATE) */
        private long totalDemands;

        /** Demands with status in (FILLED_INTERNAL, FILLED_EXTERNAL, FILLED_PARTIALLY) */
        private long filledDemands;

        /** Percentage filled: (filledDemands / totalDemands) * 100; 0 if totalDemands == 0 */
        private double percentageFilled;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InternalVsExternalSplit {
        /** Count of demands with closureReason = 'FILLED_INTERNAL' */
        private long filledInternal;

        /** Count of demands with closureReason = 'FILLED_EXTERNAL' */
        private long filledExternal;

        /** Percentage internal: (filledInternal / (filledInternal + filledExternal)) * 100 */
        private double percentageInternal;

        /** Percentage external: (filledExternal / (filledInternal + filledExternal)) * 100 */
        private double percentageExternal;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CapacityByProjectClient {
        /** Project ID from the demand */
        private Long projectId;

        /** Account/Client ID (stored as clientId for capacity reporting) */
        private Long clientId;

        /** Count of demands for this project-client pair */
        private long demandCount;
    }
}

