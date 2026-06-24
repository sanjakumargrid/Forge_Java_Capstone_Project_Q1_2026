package com.talentgrid.workforce.aiupskill.client;

import com.talentgrid.workforce.aiupskill.dto.Team5EngineerApiDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "team5-engineer-service", url = "${feign.client.team5.url:http://localhost:8091}")
public interface Team5EngineerClient {

    @GetMapping("/api/v1/engineer-profile/employees/{id}")
    Team5EngineerApiDto getEngineerById(@PathVariable("id") String id);
}

