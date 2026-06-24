package com.talentgrid.demand.ai;

public interface AiTextGenerationProvider {

    String name();

    String generateContent(String systemPrompt, String userPrompt);
}
