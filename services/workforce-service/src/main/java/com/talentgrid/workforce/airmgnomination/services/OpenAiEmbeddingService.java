package com.talentgrid.workforce.airmgnomination.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * PROD-profile embedding service backed by OpenAI's Embeddings API.
 *
 * <p>Calls {@code POST https://api.openai.com/v1/embeddings} using the configured API key and model.
 * Default model: {@code text-embedding-3-small} (1536-dim, fast, cost-effective).
 *
 * <p>Required environment variable:
 * <pre>
 *   OPENAI_API_KEY=sk-...
 * </pre>
 *
 * <p>No Spring AI dependency is required — uses the same plain {@link RestTemplate} as the Ollama service.
 */
@Slf4j
@Service
@Profile("prod")
public class OpenAiEmbeddingService implements EmbeddingService {

    private static final String OPENAI_EMBEDDINGS_URL = "https://api.openai.com/v1/embeddings";

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String model;

    public OpenAiEmbeddingService(
            RestTemplate restTemplate,
            @Value("${ai.openai.api-key}") String apiKey,
            @Value("${ai.openai.model:text-embedding-3-small}") String model) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public float[] embed(String text) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, String> requestBody = Map.of(
                "model", model,
                "input", text
        );

        HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);

        try {
            log.debug("[OPENAI-EMBED] Calling OpenAI | model={} | textLength={}", model, text.length());

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                    OPENAI_EMBEDDINGS_URL, request, Map.class);

            if (response == null || !response.containsKey("data")) {
                throw new EmbeddingException("OpenAI returned null or missing 'data' field");
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");

            if (data == null || data.isEmpty()) {
                throw new EmbeddingException("OpenAI 'data' array is empty");
            }

            @SuppressWarnings("unchecked")
            List<Double> embedding = (List<Double>) data.get(0).get("embedding");

            float[] vector = new float[embedding.size()];
            for (int i = 0; i < embedding.size(); i++) {
                vector[i] = embedding.get(i).floatValue();
            }

            log.debug("[OPENAI-EMBED] Success | model={} | dimensions={}", model, vector.length);
            return vector;

        } catch (RestClientException ex) {
            throw new EmbeddingException(
                    "OpenAI embedding call failed | model=" + model + " | error=" + ex.getMessage(),
                    ex
            );
        }
    }
}
