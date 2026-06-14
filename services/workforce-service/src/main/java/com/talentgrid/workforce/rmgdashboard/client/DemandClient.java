package com.talentgrid.workforce.rmgdashboard.client;

import com.talentgrid.workforce.rmgdashboard.dto.DemandDto;
import com.talentgrid.workforce.rmgdashboard.dto.DemandSummaryPageResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "demand-service",
        url = "${demand-service.url:http://localhost:8081}")
public interface DemandClient {

    @GetMapping("/api/demands")
    DemandSummaryPageResponse getDemandsByStatus(
            @RequestParam("status") String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "500") int size
    );

    @GetMapping("/api/demands/{id}")
    DemandDto getDemandById(@PathVariable("id") Long demandId);

    @PatchMapping("/api/demands/{id}/status")
    DemandDto updateDemandStatus(@PathVariable("id") Long demandId, @RequestBody com.talentgrid.workforce.rmgdashboard.dto.DemandStatusTransitionRequest statusUpdate);

}
