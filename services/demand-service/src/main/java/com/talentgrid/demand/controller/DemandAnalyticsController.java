package com.talentgrid.demand.controller;

import com.talentgrid.demand.dto.response.DemandAnalyticsResponse;
import com.talentgrid.demand.service.DemandAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for demand analytics dashboard.
 *
 * <p>
 * Endpoints:
 * <ul>
 * <li>{@code GET /analytics/demands} — demand analytics with fill rate, avg
 * time-to-fill,
 * status breakdown, and internal vs external fill split</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class DemandAnalyticsController {

    private final DemandAnalyticsService analyticsService;

    /**
     * Returns the demand analytics dashboard response with aggregate metrics.
     *
     * @return analytics response with fill rate, avg time-to-fill,
     *         status counts, and internal vs external split
     */
    @GetMapping("/demands")
    @PreAuthorize("hasAuthority('ANALYTICS_DEMAND_VIEW')")
    public ResponseEntity<DemandAnalyticsResponse> getDemandAnalytics() {
        DemandAnalyticsResponse response = analyticsService.getAnalytics();
        return ResponseEntity.ok(response);
    }
}
