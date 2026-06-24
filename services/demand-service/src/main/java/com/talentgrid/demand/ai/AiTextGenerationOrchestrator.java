package com.talentgrid.demand.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AiTextGenerationOrchestrator {

    private final List<AiTextGenerationProvider> providers;
    private final Map<String, SimpleCircuitBreaker> circuitBreakers;
    private final String primaryProviderName;

    public AiTextGenerationOrchestrator(
            GeminiTextGenerationProvider geminiProvider,
            GroqTextGenerationProvider groqProvider,
            @Value("${ai.failover.failure-threshold:3}") int failureThreshold,
            @Value("${ai.failover.open-duration-seconds:120}") long openDurationSeconds) {

        this.providers = List.of(geminiProvider, groqProvider);
        this.primaryProviderName = geminiProvider.name();
        this.circuitBreakers = new HashMap<>();
        Duration openDuration = Duration.ofSeconds(openDurationSeconds);

        for (AiTextGenerationProvider provider : providers) {
            circuitBreakers.put(provider.name(), new SimpleCircuitBreaker(failureThreshold, openDuration));
        }
    }

    public String generateContent(String systemPrompt, String userPrompt) {
        List<String> failures = new ArrayList<>();

        for (AiTextGenerationProvider provider : providers) {
            SimpleCircuitBreaker breaker = circuitBreakers.get(provider.name());

            if (breaker.isOpen()) {
                log.warn("Skipping AI provider '{}' — circuit breaker is open", provider.name());
                failures.add(provider.name() + ": circuit breaker open");
                continue;
            }

            try {
                String result = provider.generateContent(systemPrompt, userPrompt);
                breaker.recordSuccess();

                if (!provider.name().equals(primaryProviderName)) {
                    log.warn("Serving text generation from failover provider '{}' (primary '{}' unavailable)",
                            provider.name(), primaryProviderName);
                }

                return result;
            } catch (AiProviderException e) {
                breaker.recordFailure();
                log.warn("AI provider '{}' failed: {}", provider.name(), e.getMessage());
                failures.add(provider.name() + ": " + e.getMessage());
            }
        }

        throw new AllAiProvidersFailedException(
                "All text generation providers failed or were skipped: " + String.join("; ", failures));
    }
}
