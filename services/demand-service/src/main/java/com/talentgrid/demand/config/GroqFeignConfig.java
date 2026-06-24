package com.talentgrid.demand.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

/**
 * Feign configuration ONLY for GroqAiClient.
 *
 * <p>Adds Bearer token auth for Groq's OpenAI-compatible API (groq.com).
 * This config is NOT registered as a global @Configuration — it is referenced
 * explicitly in the @FeignClient(configuration = ...) annotation so it does NOT
 * apply to GeminiAiClient or UserAuthServiceClient.
 */
public class GroqFeignConfig {

    @Bean
    public RequestInterceptor groqBearerAuthInterceptor(@Value("${groq.api.key}") String apiKey) {
        return requestTemplate -> {
            if (apiKey != null && !apiKey.isBlank()) {
                requestTemplate.header("Authorization", "Bearer " + apiKey);
            }
        };
    }
}
