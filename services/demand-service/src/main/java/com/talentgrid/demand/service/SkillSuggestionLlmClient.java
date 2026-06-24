package com.talentgrid.demand.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talentgrid.demand.ai.AiTextGenerationOrchestrator;
import com.talentgrid.demand.ai.AllAiProvidersFailedException;
import com.talentgrid.demand.domain.entity.Skill;
import com.talentgrid.demand.dto.response.AiSkillSuggestionResponse;
import com.talentgrid.demand.dto.response.SkillDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class SkillSuggestionLlmClient {

    private final AiTextGenerationOrchestrator aiTextGenerationOrchestrator;
    private final SkillSuggestionPromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;

    public AiSkillSuggestionResponse suggestSkills(
            String jobDescriptionText,
            String jobTitle,
            String level,
            Integer experienceYears,
            List<Skill> candidates) {
        String systemPrompt = promptBuilder.buildSystemPrompt();
        String userPrompt = promptBuilder.buildUserPrompt(jobDescriptionText, jobTitle, level, experienceYears, candidates);

        if (candidates == null || candidates.isEmpty()) {
            log.warn("No candidate skills provided — skipping LLM call");
            return new AiSkillSuggestionResponse(new ArrayList<>(), new ArrayList<>());
        }

        String jsonContent;
        try {
            jsonContent = aiTextGenerationOrchestrator.generateContent(systemPrompt, userPrompt);
        } catch (AllAiProvidersFailedException e) {
            log.warn("All AI text generation providers failed; returning empty skill suggestions: {}", e.getMessage());
            return new AiSkillSuggestionResponse(new ArrayList<>(), new ArrayList<>());
        }

        if (jsonContent == null || jsonContent.isBlank()) {
            log.warn("Empty response from AI LLM");
            return new AiSkillSuggestionResponse(new ArrayList<>(), new ArrayList<>());
        }

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
