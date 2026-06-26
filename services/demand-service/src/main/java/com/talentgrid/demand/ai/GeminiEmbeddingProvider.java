package com.talentgrid.demand.ai;

import com.talentgrid.demand.client.GeminiAiClient;
import com.talentgrid.demand.client.dto.gemini.EmbedContentRequest;
import com.talentgrid.demand.client.dto.gemini.EmbedContentResponse;
import com.talentgrid.demand.config.EmbeddingDimensionConfig;
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
    private final EmbeddingDimensionConfig embeddingDimensionConfig;

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

        int outputDimension = embeddingDimensionConfig.getExpectedDimension();

        try {
            log.debug("Calling Gemini API to embed text (length={}, model={}, outputDimension={})",
                    text.length(), embeddingModel, outputDimension);

            EmbedContentRequest request = EmbedContentRequest.of(embeddingModel, text, outputDimension);
            String pathModel = embeddingModel.startsWith("models/") ? embeddingModel.substring(7) : embeddingModel;

            EmbedContentResponse response = geminiAiClient.embedContent(pathModel, apiKey, request);

            if (response != null && response.getEmbedding() != null && response.getEmbedding().getValues() != null) {
                float[] values = response.getEmbedding().getValues();
                if (values.length == 0) {
                    throw new AiProviderException("Gemini returned empty embedding vector");
                }

                log.info("Gemini embedding: textLength={}, dimension={}, model={}, provider={}",
                        text.length(), values.length, embeddingModel, PROVIDER_NAME);

                embeddingDimensionConfig.validate(values,
                        "Gemini embedding for text length " + text.length());

                return values;
            }

            throw new AiProviderException("Gemini returned empty embedding response");
        } catch (AiProviderException e) {
            throw e;
        } catch (IllegalStateException e) {
            throw new AiProviderException(e.getMessage(), e);
        } catch (Exception e) {
            throw new AiProviderException("Gemini embedding failed: " + e.getMessage(), e);
        }
    }
}
