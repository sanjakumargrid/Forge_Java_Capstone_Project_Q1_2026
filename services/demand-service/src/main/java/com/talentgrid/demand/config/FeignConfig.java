package com.talentgrid.demand.config;

import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for OpenFeign clients.
 *
 * <p>Acts as a placeholder for centralized Feign configuration.
 *
 * <p>JWT propagation for user-auth-service ONLY is handled in {@link UserAuthFeignConfig},
 * which is referenced via @FeignClient(configuration = UserAuthFeignConfig.class).
 * GeminiAiClient has no interceptors — it uses ?key= query param for Gemini API auth.
 */
@Configuration
public class FeignConfig {
    // Intentionally empty — no global RequestInterceptor beans here.
    // Global interceptors apply to ALL Feign clients, including GeminiAiClient,
    // which would break Gemini API calls by sending a Bearer JWT to Google.
}
