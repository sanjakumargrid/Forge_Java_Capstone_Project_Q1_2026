package com.talentgrid.workforce.benchreport.controller;

import com.talentgrid.workforce.benchreport.service.BenchReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bench-report")
@Tag(name = "Bench Report", description = "Bench availability grouped by upcoming windows")
public class BenchReportController {

    private final BenchReportService benchReportService;

    public BenchReportController(BenchReportService benchReportService) {
        this.benchReportService = benchReportService;
    }

    @Operation(summary = "Get bench report grouped by availability window",
            description = "Returns engineers grouped by availability window: under 30 days, 30–60 days, and 60–90 days. "
                    + "Optional page and size query parameters paginate each window independently (defaults: page=0, size=10).")
    @GetMapping
    @PreAuthorize("hasAuthority('WORKFORCE_BENCH_SEARCH')")
    public ResponseEntity<?> getBenchReport(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        if (page == null && size == null) {
            return ResponseEntity.ok(benchReportService.getBenchReport());
        }
        int resolvedPage = page != null ? page : 0;
        int resolvedSize = size != null ? size : 10;
        return ResponseEntity.ok(benchReportService.getBenchReport(resolvedPage, resolvedSize));
    }
}
