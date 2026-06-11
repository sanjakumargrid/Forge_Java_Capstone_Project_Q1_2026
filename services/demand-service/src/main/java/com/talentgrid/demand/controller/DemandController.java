package com.talentgrid.demand.controller;

import com.talentgrid.demand.service.DemandService;
import com.talentgrid.kafka.events.demand.DemandPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/demands")
@RequiredArgsConstructor
public class DemandController {

    private final DemandService demandService;

    @PostMapping
    public String createDemand(
            @RequestBody DemandPayload request,
            @RequestHeader(value = "X-Request-Id", required = false)
            String requestId) {

        return demandService.createDemand(
                request,
                requestId
        );
    }
}