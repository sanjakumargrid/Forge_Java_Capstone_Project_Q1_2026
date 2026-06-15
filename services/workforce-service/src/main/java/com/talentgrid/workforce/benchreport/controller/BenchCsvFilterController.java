package com.talentgrid.workforce.benchreport.controller;

import com.talentgrid.workforce.benchreport.dto.BenchFilterPageResponse;
import com.talentgrid.workforce.benchreport.dto.BenchFilterRequest;
import com.talentgrid.workforce.benchreport.service.BenchCsvFilterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bench-filter")
@Tag(name = "Bench CSV Filter", description = "Search, filter, and export bench employees")
public class BenchCsvFilterController {

    private final BenchCsvFilterService benchCsvFilterService;

    public BenchCsvFilterController(BenchCsvFilterService benchCsvFilterService) {
        this.benchCsvFilterService = benchCsvFilterService;
    }

    @Operation(summary = "Search and filter bench employees", description = "Returns paginated list of filtered bench employees")
    @GetMapping("/search")
    @PreAuthorize("hasAuthority('WORKFORCE_BENCH_SEARCH')")
    public ResponseEntity<BenchFilterPageResponse> searchBenchEmployees(
            BenchFilterRequest filterRequest,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "availabilityDate") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection) {
        
        BenchFilterPageResponse response = benchCsvFilterService.searchBenchEmployees(filterRequest, page, size, sortBy, sortDirection);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Export bench employees as CSV", description = "Downloads filtered bench employees as a CSV file")
    @GetMapping("/export/csv")
    @PreAuthorize("hasAuthority('WORKFORCE_REPORT_EXPORT')")
    public ResponseEntity<byte[]> exportBenchEmployeesCsv(
            BenchFilterRequest filterRequest,
            @RequestParam(defaultValue = "availabilityDate") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection) {
        
        byte[] csvData = benchCsvFilterService.exportBenchEmployeesCsv(filterRequest, sortBy, sortDirection);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "bench_employees.csv");
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
        
        return new ResponseEntity<>(csvData, headers, HttpStatus.OK);
    }
}
