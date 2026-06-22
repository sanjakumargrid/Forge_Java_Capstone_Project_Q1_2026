package com.talentgrid.demand.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talentgrid.demand.client.GeminiAiClient;
import com.talentgrid.demand.client.dto.gemini.GenerateContentRequest;
import com.talentgrid.demand.client.dto.gemini.GenerateContentResponse;
import com.talentgrid.demand.domain.entity.Skill;
import com.talentgrid.demand.dto.response.AiSkillSuggestionResponse;
import com.talentgrid.demand.dto.response.SkillDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class SkillSuggestionLlmClient {

    private final GeminiAiClient geminiAiClient;
    private final SkillSuggestionPromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.chat.model}")
    private String chatModel;

    public AiSkillSuggestionResponse suggestSkills(
            String jobDescriptionText,
            String jobTitle,
            String level,
            Integer experienceYears,
            List<Skill> candidates) {
        String systemPrompt = promptBuilder.buildSystemPrompt();
        String userPrompt = promptBuilder.buildUserPrompt(jobDescriptionText, jobTitle, level, experienceYears, candidates);

        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Gemini API key is not set (GEMINI_API_KEY); returning empty skill suggestions");
            return new AiSkillSuggestionResponse(new ArrayList<>(), new ArrayList<>());
        }

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
            log.warn("Empty response from Gemini LLM");
            return new AiSkillSuggestionResponse(new ArrayList<>(), new ArrayList<>());
        }

        String jsonContent = response.getCandidates().get(0).getContent().getParts().get(0).getText();
        try {
            // Strip potential markdown code block syntax if present
            if (jsonContent.startsWith("```json")) {
                jsonContent = jsonContent.substring(7);
                if (jsonContent.endsWith("```")) {
                    jsonContent = jsonContent.substring(0, jsonContent.length() - 3);
                }
            }
            
            AiSkillSuggestionResponse suggestion = objectMapper.readValue(jsonContent, AiSkillSuggestionResponse.class);
            return validateAgainstCandidates(suggestion, candidates);
        } catch (Exception e) {
            log.error("Failed to parse LLM response to JSON: {}", jsonContent, e);
            return new AiSkillSuggestionResponse(new ArrayList<>(), new ArrayList<>());
        }
    }

    private AiSkillSuggestionResponse validateAgainstCandidates(AiSkillSuggestionResponse response, List<Skill> candidates) {
        Set<Long> candidateIds = candidates.stream().map(Skill::getSkillId).collect(Collectors.toSet());

        List<SkillDto> validMandatory = new ArrayList<>();
        if (response.getMandatorySkills() != null) {
            for (SkillDto skill : response.getMandatorySkills()) {
                if (candidateIds.contains(skill.getSkillId())) {
                    validMandatory.add(skill);
                } else {
                    log.warn("Filtering out invalid mandatory skill from LLM: {} ({})", skill.getSkillName(), skill.getSkillId());
                }
            }
        }

        List<SkillDto> validOptional = new ArrayList<>();
        if (response.getOptionalSkills() != null) {
            for (SkillDto skill : response.getOptionalSkills()) {
                if (candidateIds.contains(skill.getSkillId()) && !containsId(validMandatory, skill.getSkillId())) {
                    validOptional.add(skill);
                } else {
                    log.warn("Filtering out invalid or duplicate optional skill from LLM: {} ({})", skill.getSkillName(), skill.getSkillId());
                }
            }
        }

        return new AiSkillSuggestionResponse(validMandatory, validOptional);
    }
    
    private boolean containsId(List<SkillDto> list, Long id) {
        return list.stream().anyMatch(s -> s.getSkillId().equals(id));
    }
}
