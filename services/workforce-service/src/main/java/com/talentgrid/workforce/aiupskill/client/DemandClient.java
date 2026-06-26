package com.talentgrid.workforce.aiupskill.client;

import com.talentgrid.workforce.aiupskill.dto.DemandPageResponse;
import com.talentgrid.workforce.aiupskill.dto.DemandApiDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "team1-demand-service", url = "${demand-service.url:http://localhost:8082}")
public interface DemandClient {

    @GetMapping("/api/v1/demands")
    DemandPageResponse getDemandsPage(@RequestParam(value = "size", defaultValue = "500") int size);

    @GetMapping("/api/v1/demands/{id}")
    DemandApiDto getDemandById(@PathVariable("id") Long id);
}
