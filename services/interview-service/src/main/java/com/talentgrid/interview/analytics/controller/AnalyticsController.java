package com.talentgrid.interview.analytics.controller;

import com.talentgrid.interview.analytics.dto.AnalyticsResponse;
import com.talentgrid.interview.analytics.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
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
            @RequestParam(value = "job_posting_id", required = false) Long jobPostingId,
            @RequestParam(value = "demandId", required = false) Long demandId) {

        Long resolvedJobPostingId = jobPostingId != null ? jobPostingId : demandId;
        AnalyticsResponse response = analyticsService.calculateLiveMetrics(resolvedJobPostingId);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/analytics/snapshot
     * Instantly serves high-speed pre-cached data snapshots from the reporting table.
     * Use this endpoint for heavy traffic dashboard views to protect database performance.
     */
    @GetMapping("/snapshot")
    public ResponseEntity<AnalyticsResponse> getCachedAnalytics(
            @RequestParam(value = "job_posting_id", required = false) Long jobPostingId,
            @RequestParam(value = "demandId", required = false) Long demandId) {

        Long resolvedJobPostingId = jobPostingId != null ? jobPostingId : demandId;
        AnalyticsResponse response = analyticsService.getHistoricalSnapshot(resolvedJobPostingId);
        return ResponseEntity.ok(response);
    }
}
