package com.talentgrid.demand.ai;

import com.talentgrid.demand.client.TalentGridAiClient;
import com.talentgrid.demand.client.dto.aigw.AiLlmCompleteRequest;
import com.talentgrid.demand.client.dto.aigw.AiLlmCompleteResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TalentGridAiTextGenerationProvider implements AiTextGenerationProvider {

    private static final String PROVIDER_NAME = "talentgrid-ai-service";

    private final TalentGridAiClient aiClient;

    @Value("${ai.service.llm.model:gpt-4o}")
    private String llmModel;

    @Value("${ai.service.llm.feature-type:UNKNOWN}")
    private String featureType;

    public TalentGridAiTextGenerationProvider(TalentGridAiClient aiClient) {
        this.aiClient = aiClient;
    }

    @Override
    public String name() {
        return PROVIDER_NAME;
    }

    @Override
    public String generateContent(String systemPrompt, String userPrompt) {
        try {
            AiLlmCompleteRequest request = AiLlmCompleteRequest.builder()
                    .featureType(featureType)
                    .prompt(userPrompt)
                    .systemPrompt(systemPrompt)
                    .model(llmModel)
                    .build();

            AiLlmCompleteResponse response = aiClient.complete(request);

            if (response == null || !response.isSuccess()) {
                String error = response != null ? response.getError() : "null response";
                throw new AiProviderException("TalentGrid AI service LLM completion failed: " + error);
            }

            String text = response.getText();
            if (text == null || text.isBlank()) {
                throw new AiProviderException("TalentGrid AI service returned blank text content");
            }

            return text;
        } catch (AiProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new AiProviderException("TalentGrid AI text generation failed: " + e.getMessage(), e);
        }
    }
}
