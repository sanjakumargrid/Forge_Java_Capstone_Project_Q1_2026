package com.talentgrid.demand.ai;

import com.talentgrid.demand.client.GroqAiClient;
import com.talentgrid.demand.client.dto.groq.GroqChatRequest;
import com.talentgrid.demand.client.dto.groq.GroqChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Groq (groq.com) failover provider for text generation when Gemini is unavailable.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GroqTextGenerationProvider implements AiTextGenerationProvider {

    private static final String PROVIDER_NAME = "groq";

    private final GroqAiClient groqAiClient;

    @Value("${groq.api.key}")
    private String apiKey;

    @Value("${groq.chat.model}")
    private String chatModel;

    @Override
    public String name() {
        return PROVIDER_NAME;
    }

    @Override
    public String generateContent(String systemPrompt, String userPrompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new AiProviderException("Groq API key is not set (GROQ_API_KEY)");
        }

        try {
            GroqChatRequest request = new GroqChatRequest(
                    chatModel,
                    List.of(
                            new GroqChatRequest.Message("system", systemPrompt),
                            new GroqChatRequest.Message("user", userPrompt)
                    )
            );

            GroqChatResponse response = groqAiClient.generateContent(request);

            if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
                throw new AiProviderException("Groq returned empty chat completion response");
            }

            GroqChatResponse.Message message = response.getChoices().get(0).getMessage();
            if (message == null || message.getContent() == null || message.getContent().isBlank()) {
                throw new AiProviderException("Groq returned blank message content");
            }

            return message.getContent();
        } catch (AiProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new AiProviderException("Groq text generation failed: " + e.getMessage(), e);
        }
    }
}
