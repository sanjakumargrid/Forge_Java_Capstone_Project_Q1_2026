package com.talentgrid.demand.controller;

import com.talentgrid.demand.dto.request.*;
import com.talentgrid.demand.dto.response.DemandResponse;
import com.talentgrid.demand.service.DemandLifecycleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/demands/{id}/lifecycle")
public class DemandLifecycleController {

    private final DemandLifecycleService lifecycleService;

    public DemandLifecycleController(DemandLifecycleService lifecycleService) {
        this.lifecycleService = lifecycleService;
    }

    @PostMapping("/submit")
    public ResponseEntity<DemandResponse> submit(@PathVariable Long id, @RequestBody SubmitDemandRequest request) {
        return ResponseEntity.ok(lifecycleService.submit(id, request));
    }

    @PostMapping("/approve")
    public ResponseEntity<DemandResponse> approve(@PathVariable Long id, @RequestBody ApproveDemandRequest request) {
        return ResponseEntity.ok(lifecycleService.approve(id, request));
    }

    @PostMapping("/hold")
    public ResponseEntity<DemandResponse> hold(@PathVariable Long id, @RequestBody HoldDemandRequest request) {
        return ResponseEntity.ok(lifecycleService.hold(id, request));
    }

    @PostMapping("/cancel")
    public ResponseEntity<DemandResponse> cancel(@PathVariable Long id, @RequestBody CancelDemandRequest request) {
        return ResponseEntity.ok(lifecycleService.cancel(id, request));
    }
}
