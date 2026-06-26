package com.talentgrid.workforce.rmganalyticsdashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for demand count analytics.
 *
 * <ul>
 *   <li><b>totalDemandCount</b>  – all demands whose status is NOT DRAFT</li>
 *   <li><b>activeDemandCount</b> – demands in APPROVED | INTERNAL_SEARCH | OPEN_EXTERNAL</li>
 *   <li><b>openExternalCount</b> – demands in OPEN_EXTERNAL</li>
 *   <li><b>closedCount</b>       – demands in CLOSED</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemandCountAnalyticsResponse {

    /** Count of all demands excluding DRAFT status. */
    private long totalDemandCount;

    /** Count of demands with status APPROVED, INTERNAL_SEARCH, or OPEN_EXTERNAL. */
    private long activeDemandCount;

    /** Count of demands with status OPEN_EXTERNAL. */
    private long openExternalCount;

    /** Count of demands with status CLOSED. */
    private long closedCount;
}
