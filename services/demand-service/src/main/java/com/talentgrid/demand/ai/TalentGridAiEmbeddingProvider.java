package com.talentgrid.demand.ai;

import com.talentgrid.demand.client.TalentGridAiClient;
import com.talentgrid.demand.client.dto.aigw.AiEmbedRequest;
import com.talentgrid.demand.client.dto.aigw.AiEmbedResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class TalentGridAiEmbeddingProvider implements AiEmbeddingProvider {

    private static final String PROVIDER_NAME = "talentgrid-ai-service";

    private final TalentGridAiClient aiClient;

    @Value("${ai.service.embedding.model:text-embedding-3-small}")
    private String embeddingModel;

    public TalentGridAiEmbeddingProvider(TalentGridAiClient aiClient) {
        this.aiClient = aiClient;
    }

    @Override
    public String name() {
        return PROVIDER_NAME;
    }

    @Override
    public float[] embedContent(String text) {
        try {
            AiEmbedRequest request = AiEmbedRequest.builder()
                    .text(text)
                    .model(embeddingModel)
                    .build();

            AiEmbedResponse response = aiClient.embed(request);

            List<Float> embedding = response != null ? response.getEmbedding() : null;
            if (embedding == null || embedding.isEmpty()) {
                throw new AiProviderException("TalentGrid AI service returned empty embedding response");
            }

            float[] values = new float[embedding.size()];
            for (int i = 0; i < embedding.size(); i++) {
                values[i] = embedding.get(i);
            }

            return values;
        } catch (AiProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new AiProviderException("TalentGrid AI embedding failed: " + e.getMessage(), e);
        }
    }
}
