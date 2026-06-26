package com.talentgrid.workforce.rmganalyticsdashboard.controller;

import com.talentgrid.workforce.rmganalyticsdashboard.dto.*;
import com.talentgrid.workforce.rmganalyticsdashboard.service.RmgAnalyticsDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/v1/rmg-analytics-dashboard")
@RequiredArgsConstructor
@Tag(name = "RMG Analytics Dashboard", description = "RMG KPIs and bench distribution charts for deployable talent")
@PreAuthorize("hasAuthority('WORKFORCE_ANALYTICS_VIEW')")
public class RmgAnalyticsDashboardController {

    private final RmgAnalyticsDashboardService analyticsDashboardService;

    @GetMapping("/open-demands")
    @Operation(
            summary = "Get Current Open Demands",
            description = "Returns the number of currently open demands (status OPEN) from demand-service."
    )
    public ResponseEntity<OpenDemandsResponse> getCurrentOpenDemands() {
        return ResponseEntity.ok(analyticsDashboardService.getCurrentOpenDemands());
    }

    @GetMapping("/bench-strength")
    @Operation(
            summary = "Get Bench Strength",
            description = "Returns total bench strength and headcount distribution across the same availability windows "
                    + "as the bench report (under 30 days, 30–60, 60–90)."
    )
    public ResponseEntity<BenchStrengthResponse> getBenchStrength() {
        return ResponseEntity.ok(analyticsDashboardService.getBenchStrength());
    }

    @GetMapping("/bench-distribution-by-skill")
    @Operation(
            summary = "Bench distribution by skill (bar chart)",
            description = "Top skills among bench employees (active, utilisation 0). Counts are per skill tag on an employee; "
                    + "one engineer can increment multiple skills. Default top 15, max 50."
    )
    public ResponseEntity<BenchDistributionBySkillResponse> getBenchDistributionBySkill(
            @Parameter(description = "Max number of skill bars to return (1–50, default 15)")
            @RequestParam(name = "limit", required = false) Integer limit) {
        return ResponseEntity.ok(analyticsDashboardService.getBenchDistributionBySkill(limit));
    }

    @GetMapping("/bench-distribution-by-availability")
    @Operation(
            summary = "Bench distribution by availability window (bar chart)",
            description = "Bench employees (utilisation 0) split by availability date: Immediate (≤7 days from today), "
                    + "Within 2 Weeks (8–14d), Within 1 Month (15–30d), More Than 1 Month (>30d or unknown date). "
                    + "Uses internal employee availability only."
    )
    public ResponseEntity<BenchDistributionByAvailabilityResponse> getBenchDistributionByAvailability() {
        return ResponseEntity.ok(analyticsDashboardService.getBenchDistributionByAvailability());
    }

    @GetMapping("/nominations-sent")
    @Operation(
            summary = "Get Nominations Sent",
            description = "Returns nominations created in the current ISO week (UTC)."
    )
    public ResponseEntity<NominationsSentResponse> getNominationsSent() {
        return ResponseEntity.ok(analyticsDashboardService.getNominationsSent());
    }

    @GetMapping("/allocation-rate")
    @Operation(
            summary = "Get Allocation Rate",
            description = "Returns the average utilisation percentage across active internal employees."
    )
    public ResponseEntity<AllocationRateResponse> getAllocationRate() {
        return ResponseEntity.ok(analyticsDashboardService.getAllocationRate());
    }

    @GetMapping("/demands-requiring-nominations")
    @Operation(
            summary = "Get Demands Requiring Nominations",
            description = "Returns APPROVED demands where internal filled count is still below required headcount "
                    + "(required count defaults to 1 when null)."
    )
    public ResponseEntity<DemandsRequiringNominationsResponse> getDemandsRequiringNominations() {
        return ResponseEntity.ok(analyticsDashboardService.getDemandsRequiringNominations());
    }

    @GetMapping("/internal-match-success-rate")
    @Operation(
            summary = "Get Internal Match Success Rate",
            description = "Returns successful (ACCEPTED) internal matches divided by decided matches (ACCEPTED+REJECTED). "
                    + "Optional sinceDays limits the window by nomination time."
    )
    public ResponseEntity<InternalMatchSuccessRateResponse> getInternalMatchSuccessRate(
            @Parameter(description = "If set, only nominations on or after now minus this many days (UTC)")
            @RequestParam(name = "sinceDays", required = false) Integer sinceDays) {
        return ResponseEntity.ok(analyticsDashboardService.getInternalMatchSuccessRate(sinceDays));
    }

    @GetMapping("/nomination-to-decision-time")
    @Operation(
            summary = "Get Average Nomination-to-Decision Time",
            description = "Average days between nomination creation and review decision for ACCEPTED/REJECTED matches "
                    + "with a review timestamp. Optional sinceDays limits by nomination time."
    )
    public ResponseEntity<NominationToDecisionTimeResponse> getNominationToDecisionTime(
            @RequestParam(name = "sinceDays", required = false) Integer sinceDays) {
        return ResponseEntity.ok(analyticsDashboardService.getAverageNominationToDecisionTime(sinceDays));
    }

    @GetMapping("/demand-count-analytics")
    @PreAuthorize("hasAuthority('ANALYTICS_DEMAND_VIEW') or hasAuthority('DEMAND_VIEW')")
    @Operation(
            summary = "Get Demand Count Analytics",
            description = "Returns four demand-count metrics sourced from demand-service via Feign:\n"
                    + "• totalDemandCount  – all demands excluding DRAFT status\n"
                    + "• activeDemandCount – demands in APPROVED, INTERNAL_SEARCH, or OPEN_EXTERNAL\n"
                    + "• openExternalCount – demands in OPEN_EXTERNAL\n"
                    + "• closedCount       – demands in CLOSED"
    )
    public ResponseEntity<DemandCountAnalyticsResponse> getDemandCountAnalytics() {
        return ResponseEntity.ok(analyticsDashboardService.getDemandCountAnalytics());
    }
}
