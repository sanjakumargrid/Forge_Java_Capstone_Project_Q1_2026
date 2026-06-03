package com.talentgrid.demand.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "matching-service", url = "${app.services.matching.url:http://localhost:8083}")
public interface MatchingServiceClient {
    @GetMapping("/api/v1/matching/health")
    String healthCheck();
}
