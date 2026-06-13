package com.talentgrid.workforce.rmgdashboard.client;

import com.talentgrid.workforce.rmgdashboard.dto.DemandDto;
import com.talentgrid.workforce.rmgdashboard.dto.StatusUpdateRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import com.talentgrid.workforce.rmgdashboard.dto.DemandPageResponse;
@FeignClient(name = "demand-service",
        url = "${demand-service.url:http://localhost:8081}")
public interface DemandClient {

    @GetMapping("/api/demands")
    DemandPageResponse getDemandsByStatus(
            @RequestParam("status") String status,
            @RequestParam(value = "size", defaultValue = "500") int size
    );

    @PatchMapping("/api/demands/{id}/status")
    DemandDto updateDemandStatus(@PathVariable("id") Long demandId, @RequestBody com.talentgrid.workforce.rmgdashboard.dto.DemandStatusTransitionRequest statusUpdate);

}
