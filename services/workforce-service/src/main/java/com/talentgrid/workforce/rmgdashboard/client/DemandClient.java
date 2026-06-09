package com.talentgrid.workforce.rmgdashboard.client;

import com.talentgrid.workforce.rmgdashboard.dto.DemandDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "demand-service", url = "${demand-service.url:http://localhost:8081}")
public interface DemandClient {

    @GetMapping("/api/demands/status")
    List<DemandDto> getDemandsByStatus(@RequestParam("status") String status);
}
