package com.talentgrid.demand.controller;

import com.talentgrid.demand.dto.response.DemandAnalyticsMetricsResponse;
import com.talentgrid.demand.service.DemandAnalyticsService;
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
 * <li>{@code GET /api/analytics/demands} — demand fill rate (position-level),
 * average time-to-fill, and internal vs external split. Default window: last 30 days.
 * </ul>
 */
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class DemandAnalyticsController {

    private final DemandAnalyticsService analyticsService;

    /**
     * Returns position-level demand analytics metrics.
     *
     * @param startDate start of analytics window (optional, format: yyyy-MM-dd); default = 30 days ago
     * @param endDate   end of analytics window (optional, format: yyyy-MM-dd); default = today
     * @return analytics response with fill rate (position-level), avg time-to-fill, and split
     */
    @GetMapping("/demands")
    @PreAuthorize("hasAuthority('ANALYTICS_DEMAND_VIEW')")
    public ResponseEntity<DemandAnalyticsMetricsResponse> getDemandAnalytics(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate) {
        DemandAnalyticsMetricsResponse response = analyticsService.getAnalytics(startDate, endDate);
        return ResponseEntity.ok(response);
    }
}
