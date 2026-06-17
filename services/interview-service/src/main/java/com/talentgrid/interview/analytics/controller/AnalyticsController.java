package com.talentgrid.interview.analytics.controller;

import com.talentgrid.interview.analytics.dto.AnalyticsResponse;
import com.talentgrid.interview.analytics.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Allows seamless frontend connection for BL Team 4 dashboards
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    /**
     * GET /api/v1/analytics/live
     * Forces a real-time computation pass over operational application and offer tables.
     *
     * @param demandId Optional long parameter to filter performance metrics by specific job roles
     * @return REST payload matching REQ-ER-15 contract
     */
    @GetMapping("/live")
    public ResponseEntity<AnalyticsResponse> getLiveAnalytics(
            @RequestParam(value = "demand_id", required = false) Long demandId) {

        AnalyticsResponse response = analyticsService.calculateLiveMetrics(demandId);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/analytics/snapshot
     * Instantly serves high-speed pre-cached data snapshots from the reporting table.
     * Use this endpoint for heavy traffic dashboard views to protect database performance.
     */
    @GetMapping("/snapshot")
    public ResponseEntity<AnalyticsResponse> getCachedAnalytics(
            @RequestParam(value = "demand_id", required = false) Long demandId) {

        AnalyticsResponse response = analyticsService.getHistoricalSnapshot(demandId);
        return ResponseEntity.ok(response);
    }
}
