package com.talentgrid.workforce.rmgsearch.controller;

import com.talentgrid.workforce.rmgsearch.dto.RmgSearchRequest;
import com.talentgrid.workforce.rmgsearch.dto.RmgSearchResponse;
import com.talentgrid.workforce.rmgsearch.service.RmgSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/v1/rmg-search")
@Tag(name = "RMG Search", description = "Resource Management Group search — find bench employees by id, name, email, skills, level, contract type, availability, and location")
public class RmgSearchController {

    private final RmgSearchService rmgSearchService;

    public RmgSearchController(RmgSearchService rmgSearchService) {
        this.rmgSearchService = rmgSearchService;
    }

    @Operation(
            summary = "Search bench employees for resource management",
            description = "Applies multi-criteria AND filtering on employeeId, name, email, skills (OR within skills), "
                    + "level, contractType, availabilityDate range, and location. All filter parameters are optional; "
                    + "omitting a filter disables it. Repeat the skills query parameter for multiple values "
                    + "(e.g. skills=Java&skills=Python). Results are ranked by availabilityDate ascending "
                    + "(earliest available first) and paginated (defaults: page=0, size=10)."
    )
    @GetMapping("/search")
    @PreAuthorize("hasAuthority('WORKFORCE_BENCH_SEARCH')")
    public ResponseEntity<RmgSearchResponse> search(
            RmgSearchRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(rmgSearchService.search(request, page, size));
    }
}
