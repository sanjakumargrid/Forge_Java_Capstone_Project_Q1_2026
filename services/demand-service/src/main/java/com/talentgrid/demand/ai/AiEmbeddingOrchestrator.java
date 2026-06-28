package com.talentgrid.demand.ai;

import com.talentgrid.demand.config.EmbeddingDimensionConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Orchestrates embedding requests through the available provider chain.
 *
 * <p>Unlike {@link AiTextGenerationOrchestrator}, this class wraps only
 * {@link GeminiEmbeddingProvider}. No secondary embedding provider is configured
 * (Groq does not offer a compatible embeddings endpoint for this use case), so
 * there is no failover here. When Gemini embedding fails or its circuit breaker
 * is open, callers receive {@link AllAiProvidersFailedException} and should
 * degrade gracefully (e.g. {@code CandidateSkillRetrievalService} falls back to
 * {@code FULL_CATALOG} mode).
 */
@Slf4j
@Service
public class AiEmbeddingOrchestrator {

    public record EmbeddingResult(float[] values, String providerName) {}

    private final TalentGridAiEmbeddingProvider aiProvider;
    private final EmbeddingDimensionConfig embeddingDimensionConfig;
    private final SimpleCircuitBreaker circuitBreaker;

    public AiEmbeddingOrchestrator(
            TalentGridAiEmbeddingProvider aiProvider,
            EmbeddingDimensionConfig embeddingDimensionConfig,
            @Value("${ai.failover.failure-threshold:3}") int failureThreshold,
            @Value("${ai.failover.open-duration-seconds:120}") long openDurationSeconds) {

        this.aiProvider = aiProvider;
        this.embeddingDimensionConfig = embeddingDimensionConfig;
        this.circuitBreaker = new SimpleCircuitBreaker(
                failureThreshold, Duration.ofSeconds(openDurationSeconds));
    }

    public EmbeddingResult embedContent(String text) {
        if (circuitBreaker.isOpen()) {
            log.warn("Skipping AI embedding provider '{}' — circuit breaker is open", aiProvider.name());
            throw new AllAiProvidersFailedException(
                    "Embedding provider '" + aiProvider.name() + "' circuit breaker is open");
        }

        try {
            float[] values = aiProvider.embedContent(text);
            embeddingDimensionConfig.validate(values, "Embedding orchestrator");
            log.info("Embedding orchestrator: textLength={}, dimension={}, provider={}",
                    text.length(), values.length, aiProvider.name());
            circuitBreaker.recordSuccess();
            return new EmbeddingResult(values, aiProvider.name());
        } catch (AiProviderException e) {
            circuitBreaker.recordFailure();
            log.warn("AI embedding provider '{}' failed: {}", aiProvider.name(), e.getMessage());
            throw new AllAiProvidersFailedException(
                    "Embedding provider '" + aiProvider.name() + "' failed: " + e.getMessage(), e);
        }
    }
}
