package com.talentgrid.demand.client;

import com.talentgrid.demand.client.dto.groq.GroqChatRequest;
import com.talentgrid.demand.client.dto.groq.GroqChatResponse;
import com.talentgrid.demand.config.GroqFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign client for Groq's OpenAI-compatible chat completions API (groq.com).
 *
 * <p>Full URL: {@code https://api.groq.com/openai/v1/chat/completions}
 */
@FeignClient(name = "groqAiClient", url = "${groq.api.base-url}", configuration = GroqFeignConfig.class)
public interface GroqAiClient {

    @PostMapping("/chat/completions")
    GroqChatResponse generateContent(@RequestBody GroqChatRequest request);
}
