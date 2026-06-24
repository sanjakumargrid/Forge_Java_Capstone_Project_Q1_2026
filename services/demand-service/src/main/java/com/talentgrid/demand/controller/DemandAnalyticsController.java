package com.talentgrid.demand.controller;

import com.talentgrid.demand.dto.response.DemandAnalyticsMetricsResponse;
import com.talentgrid.demand.dto.response.DemandAnalyticsV1Response;
import com.talentgrid.demand.service.DemandAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * REST controller for position-level demand analytics dashboard.
 *
 * <p>
 * Endpoints:
 * <ul>
 * <li>{@code GET /api/v1/analytics/demands} — demand fill rate (position-level),
 * average time-to-fill, and internal vs external split. Default window: last 30 days.
 * <li>{@code GET /api/v1/demands/analytics} — V1 analytics with business unit filter,
 * fill rate, time-to-fill, internal vs external split, and capacity by project-client.
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/")
@RequiredArgsConstructor
@Tag(name = "Demand Analytics", description = "Demand metrics and analytics endpoints")
public class DemandAnalyticsController {

    private final DemandAnalyticsService analyticsService;
    
    /**
     * Returns V1 demand analytics metrics with optional filters and project-client capacity data.
     * Consumed by dashboard team; read-only aggregate data (no auth changes needed).
     * Filters: dateFrom, dateTo (date range), businessUnit (optional).
     *
     * <p>Metrics returned:
     * <ul>
     *   <li>fillRate: % of demands that reached {@code FILLED} / demands created in window</li>
     *   <li>avgTimeToFillDays: average days from creation to first FILLED_* status transition</li>
     *   <li>internalVsExternalSplit: counts and % breakdown by closure reason</li>
     *   <li>capacityByProjectClient: demand counts grouped by projectId + clientId</li>
     * </ul>
     *
     * <p>Performance target: p95 &lt; 500ms (indexes on status, closureReason, projectId, createdAt, businessUnit)
     *
     * @param dateFrom optional start date filter (format: yyyy-MM-dd); default = 30 days ago
     * @param dateTo optional end date filter (format: yyyy-MM-dd); default = today
     * @param businessUnit optional business unit filter
     * @return V1 analytics response with metrics and capacity data
     */
    @GetMapping("/demands/analytics")
    @PreAuthorize("hasAuthority('ANALYTICS_DEMAND_VIEW')")
    @Operation(
            summary = "Get demand analytics with capacity metrics",
            description = "Returns aggregated demand metrics including fill rate, time-to-fill, internal vs external split, and capacity by project-client. Supports optional date range and business unit filters."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Analytics metrics computed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid date format or parameters"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions for ANALYTICS_DEMAND_VIEW")
    })
    public ResponseEntity<DemandAnalyticsV1Response> getDemandAnalyticsV1(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @Parameter(description = "Start date for filter (format: yyyy-MM-dd); if not provided, defaults to 30 days before dateTo")
            LocalDate dateFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @Parameter(description = "End date for filter (format: yyyy-MM-dd); if not provided, defaults to today")
            LocalDate dateTo,
            @RequestParam(required = false)
            @Parameter(description = "Optional business unit filter; if provided, only demands matching this BU are included in metrics")
            String businessUnit) {
        DemandAnalyticsV1Response response = analyticsService.getAnalyticsV1(dateFrom, dateTo, businessUnit);
        return ResponseEntity.ok(response);
    }
}
