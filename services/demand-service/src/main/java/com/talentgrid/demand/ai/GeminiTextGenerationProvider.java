package com.talentgrid.demand.ai;

import com.talentgrid.demand.client.GeminiAiClient;
import com.talentgrid.demand.client.dto.gemini.GenerateContentRequest;
import com.talentgrid.demand.client.dto.gemini.GenerateContentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiTextGenerationProvider implements AiTextGenerationProvider {

    private static final String PROVIDER_NAME = "gemini";

    private final GeminiAiClient geminiAiClient;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.chat.model}")
    private String chatModel;

    @Override
    public String name() {
        return PROVIDER_NAME;
    }

    @Override
    public String generateContent(String systemPrompt, String userPrompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new AiProviderException("Gemini API key is not set (GEMINI_API_KEY)");
        }

        try {
            GenerateContentRequest request = new GenerateContentRequest();

            GenerateContentRequest.Content systemInstruction = new GenerateContentRequest.Content(
                    "system", List.of(new GenerateContentRequest.Part(systemPrompt)));
            request.setSystemInstruction(systemInstruction);

            GenerateContentRequest.Content userContent = new GenerateContentRequest.Content(
                    "user", List.of(new GenerateContentRequest.Part(userPrompt)));
            request.setContents(List.of(userContent));

            String pathModel = chatModel.startsWith("models/") ? chatModel.substring(7) : chatModel;
            GenerateContentResponse response = geminiAiClient.generateContent(pathModel, apiKey, request);

            if (response == null || response.getCandidates() == null || response.getCandidates().isEmpty()) {
                throw new AiProviderException("Gemini returned empty generateContent response");
            }

            String text = response.getCandidates().get(0).getContent().getParts().get(0).getText();
            if (text == null || text.isBlank()) {
                throw new AiProviderException("Gemini returned blank text content");
            }

            return text;
        } catch (AiProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new AiProviderException("Gemini text generation failed: " + e.getMessage(), e);
        }
    }
}
