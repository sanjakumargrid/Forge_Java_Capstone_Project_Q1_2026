package com.talentgrid.demand.ai;

import com.talentgrid.demand.client.GeminiAiClient;
import com.talentgrid.demand.client.dto.gemini.EmbedContentRequest;
import com.talentgrid.demand.client.dto.gemini.EmbedContentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiEmbeddingProvider implements AiEmbeddingProvider {

    private static final String PROVIDER_NAME = "gemini";

    private final GeminiAiClient geminiAiClient;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.embedding.model}")
    private String embeddingModel;

    @Override
    public String name() {
        return PROVIDER_NAME;
    }

    @Override
    public float[] embedContent(String text) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new AiProviderException("Gemini API key is not set (GEMINI_API_KEY)");
        }

        try {
            log.debug("Calling Gemini API to embed text: {}", text.substring(0, Math.min(50, text.length())));

            EmbedContentRequest request = EmbedContentRequest.of(embeddingModel, text);
            String pathModel = embeddingModel.startsWith("models/") ? embeddingModel.substring(7) : embeddingModel;

            EmbedContentResponse response = geminiAiClient.embedContent(pathModel, apiKey, request);

            if (response != null && response.getEmbedding() != null && response.getEmbedding().getValues() != null) {
                float[] values = response.getEmbedding().getValues();
                if (values.length == 0) {
                    throw new AiProviderException("Gemini returned empty embedding vector");
                }
                return values;
            }

            throw new AiProviderException("Gemini returned empty embedding response");
        } catch (AiProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new AiProviderException("Gemini embedding failed: " + e.getMessage(), e);
        }
    }
}
