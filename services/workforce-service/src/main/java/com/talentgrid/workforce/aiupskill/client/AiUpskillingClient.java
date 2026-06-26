package com.talentgrid.workforce.aiupskill.client;

import com.talentgrid.workforce.aiupskill.dto.MissingSkillsRequest;
import com.talentgrid.workforce.aiupskill.dto.UpskillingRecommendationResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "ai-upskilling-service", url = "${feign.client.ai.url:http://localhost:8090}")
public interface AiUpskillingClient {

    @PostMapping("/v1/ai/upskilling/recommendations")
    UpskillingRecommendationResponse getRecommendations(@RequestBody MissingSkillsRequest request);
}