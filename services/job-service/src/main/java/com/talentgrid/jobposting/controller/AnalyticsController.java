package com.talentgrid.jobposting.controller;

import com.talentgrid.jobposting.dto.request.ChannelEventRequest;
import com.talentgrid.jobposting.dto.response.MarketPresenceResponse;
import com.talentgrid.jobposting.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REQ-AN-03 — Market Presence analytics API.
 *
 * Exposes job posting views, clicks, apply-starts and completions per channel for the
 * BL Team 3 market-presence dashboard, plus an ingestion endpoint for channels to report events.
 */
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Market presence dashboard — funnel metrics per channel (REQ-AN-03)")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    /** Aggregated market-presence funnel across all job postings, broken down by channel. */
    @GetMapping("/market-presence")
    @Operation(summary = "Market presence dashboard",
            description = "Returns views, clicks, apply-starts and completions per channel, plus totals, "
                    + "aggregated across all job postings. Consumed by BL Team 3 analytics.")
    public ResponseEntity<MarketPresenceResponse> getMarketPresence() {
        return ResponseEntity.ok(analyticsService.getMarketPresence());
    }

    /** Market-presence funnel for one job posting, broken down by channel. */
    @GetMapping("/market-presence/{jobPostingId}")
    @Operation(summary = "Market presence for a single posting",
            description = "Returns the per-channel funnel breakdown for one job posting.")
    public ResponseEntity<MarketPresenceResponse> getMarketPresenceForPosting(
            @PathVariable Long jobPostingId) {
        return ResponseEntity.ok(analyticsService.getMarketPresenceForPosting(jobPostingId));
    }

    /** Records a single funnel event (view/click/apply-start/apply-completion) for a channel. */
    @PostMapping("/events")
    @Operation(summary = "Record a channel funnel event",
            description = "Increments the matching per-channel counter for a job posting.")
    public ResponseEntity<Map<String, String>> recordEvent(
            @Valid @RequestBody ChannelEventRequest request) {
        analyticsService.recordEvent(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(Map.of("status", "recorded"));
    }
}
