package com.talentgrid.demand.service;

import com.talentgrid.demand.client.GeminiAiClient;
import com.talentgrid.demand.client.dto.gemini.EmbedContentRequest;
import com.talentgrid.demand.client.dto.gemini.EmbedContentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiEmbeddingService {

    private final GeminiAiClient geminiAiClient;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.embedding.model}")
    private String embeddingModel;

    public float[] embedContent(String text) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Gemini API key is not set (GEMINI_API_KEY); skipping embedding");
            return new float[0];
        }
        log.debug("Calling Gemini API to embed text: {}", text.substring(0, Math.min(50, text.length())));
        
        EmbedContentRequest request = EmbedContentRequest.of(embeddingModel, text);
        
        // Strip "models/" if present because the feign client already has "{model}:embedContent" 
        // Wait, the Gemini API path is /v1beta/models/embedding-001:embedContent
        String pathModel = embeddingModel.startsWith("models/") ? embeddingModel.substring(7) : embeddingModel;
        
        EmbedContentResponse response = geminiAiClient.embedContent(pathModel, apiKey, request);
        
        if (response != null && response.getEmbedding() != null && response.getEmbedding().getValues() != null) {
            return response.getEmbedding().getValues();
        }
        
        log.warn("Gemini API returned empty embedding response");
        return new float[0];
    }
}
