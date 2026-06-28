package com.talentgrid.demand.client;

import com.talentgrid.demand.client.dto.aigw.AiEmbedRequest;
import com.talentgrid.demand.client.dto.aigw.AiEmbedResponse;
import com.talentgrid.demand.client.dto.aigw.AiLlmCompleteRequest;
import com.talentgrid.demand.client.dto.aigw.AiLlmCompleteResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "talentgrid-ai-client", url = "${ai.service.base-url:http://localhost:8084}")
public interface TalentGridAiClient {

    @PostMapping("/api/t4/v1/ai/llm/complete")
    AiLlmCompleteResponse complete(@RequestBody AiLlmCompleteRequest request);

    @PostMapping("/api/t4/v1/ai/llm/embed")
    AiEmbedResponse embed(@RequestBody AiEmbedRequest request);
}
