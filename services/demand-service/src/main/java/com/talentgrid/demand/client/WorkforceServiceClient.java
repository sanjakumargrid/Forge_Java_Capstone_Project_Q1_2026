package com.talentgrid.demand.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "workforce-service", url = "${app.services.workforce.url:http://localhost:8082}")
public interface WorkforceServiceClient {
    @GetMapping("/api/v1/workforce/health")
    String healthCheck();
}
