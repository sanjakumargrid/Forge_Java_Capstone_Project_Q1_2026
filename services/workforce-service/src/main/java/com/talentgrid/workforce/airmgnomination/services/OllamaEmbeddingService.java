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
 * DEV-profile embedding service backed by a locally running Ollama server.
 *
 * <p>Calls {@code POST {ai.ollama.base-url}/api/embeddings} with the configured model.
 * Default model: {@code nomic-embed-text} (768-dim, free, runs offline).
 *
 * <p>Start Ollama before running the app:
 * <pre>
 *   ollama pull nomic-embed-text
 *   ollama serve
 * </pre>
 */
@Slf4j
@Service
@Profile("dev")
public class OllamaEmbeddingService implements EmbeddingService {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String model;

    public OllamaEmbeddingService(
            RestTemplate restTemplate,
            @Value("${ai.ollama.base-url:http://localhost:11434}") String baseUrl,
            @Value("${ai.ollama.model:nomic-embed-text}") String model) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.model = model;
    }

    @Override
    public float[] embed(String text) {
        String url = baseUrl + "/api/embeddings";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> requestBody = Map.of(
                "model", model,
                "prompt", text
        );

        HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);

        try {
            log.debug("[OLLAMA-EMBED] Calling {} | model={} | textLength={}", url, model, text.length());

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);

            if (response == null || !response.containsKey("embedding")) {
                throw new EmbeddingException("Ollama returned null or missing 'embedding' field");
            }

            @SuppressWarnings("unchecked")
            List<Double> embedding = (List<Double>) response.get("embedding");

            float[] vector = new float[embedding.size()];
            for (int i = 0; i < embedding.size(); i++) {
                vector[i] = embedding.get(i).floatValue();
            }

            log.debug("[OLLAMA-EMBED] Success | model={} | dimensions={}", model, vector.length);
            return vector;

        } catch (RestClientException ex) {
            throw new EmbeddingException(
                    "Ollama embedding call failed | url=" + url + " | model=" + model + " | error=" + ex.getMessage(),
                    ex
            );
        }
    }
}
