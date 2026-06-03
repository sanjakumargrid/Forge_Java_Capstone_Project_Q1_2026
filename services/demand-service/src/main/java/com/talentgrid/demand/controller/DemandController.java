package com.talentgrid.demand.controller;

import com.talentgrid.demand.dto.request.CreateDemandRequest;
import com.talentgrid.demand.dto.request.UpdateDemandRequest;
import com.talentgrid.demand.dto.response.DemandResponse;
import com.talentgrid.demand.dto.response.DemandSummaryResponse;
import com.talentgrid.demand.service.DemandService;
import com.talentgrid.demand.service.DemandQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/demands")
public class DemandController {

    private final DemandService demandService;
    private final DemandQueryService demandQueryService;

    public DemandController(DemandService demandService, DemandQueryService demandQueryService) {
        this.demandService = demandService;
        this.demandQueryService = demandQueryService;
    }

    @PostMapping
    public ResponseEntity<DemandResponse> createDemand(@RequestBody CreateDemandRequest request) {
        return ResponseEntity.ok(demandService.createDemand(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DemandResponse> updateDemand(@PathVariable Long id, @RequestBody UpdateDemandRequest request) {
        return ResponseEntity.ok(demandService.updateDemand(id, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DemandResponse> getDemandById(@PathVariable Long id) {
        return ResponseEntity.ok(demandQueryService.getDemandById(id));
    }

    @GetMapping
    public ResponseEntity<List<DemandSummaryResponse>> getAllDemands() {
        return ResponseEntity.ok(demandQueryService.getAllDemands());
    }
}
