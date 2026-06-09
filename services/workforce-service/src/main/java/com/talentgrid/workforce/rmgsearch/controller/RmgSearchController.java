package com.talentgrid.workforce.rmgsearch.controller;

import com.talentgrid.workforce.rmgsearch.dto.RmgSearchRequest;
import com.talentgrid.workforce.rmgsearch.dto.RmgSearchResponse;
import com.talentgrid.workforce.rmgsearch.service.RmgSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rmg-search")
@Tag(name = "RMG Search", description = "Resource Management Group search — find bench employees by skill, availability, location, seniority, and contract type")
public class RmgSearchController {

    private final RmgSearchService rmgSearchService;

    public RmgSearchController(RmgSearchService rmgSearchService) {
        this.rmgSearchService = rmgSearchService;
    }

    @Operation(
            summary = "Search bench employees for resource management",
            description = "Applies multi-criteria AND filtering on skill, availabilityDate range, location, seniority, "
                    + "and contractType. All parameters are optional; omitting a parameter disables that filter. "
                    + "Results are ranked by availabilityDate ascending (earliest available first)."
    )
    @GetMapping("/search")
    public ResponseEntity<RmgSearchResponse> search(RmgSearchRequest request) {
        return ResponseEntity.ok(rmgSearchService.search(request));
    }
}
