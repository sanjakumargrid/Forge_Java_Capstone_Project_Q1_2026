package com.talentgrid.workforce.common.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Provides a shared {@link RestTemplate} bean for HTTP calls within this service.
 *
 * <p>Currently used by:
 * <ul>
 *   <li>{@code OllamaEmbeddingService} (dev) — calls local Ollama server</li>
 *   <li>{@code OpenAiEmbeddingService} (prod) — calls OpenAI REST API</li>
 * </ul>
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(60))  // Embedding models can be slow on first run
                .build();
    }
}
