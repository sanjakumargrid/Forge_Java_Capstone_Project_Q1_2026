package com.talentgrid.demand.service;


import com.talentgrid.demand.domain.entity.Skill;
import com.talentgrid.demand.dto.response.AiSkillSuggestionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiSkillSuggestionService {

    private final CandidateSkillRetrievalService candidateSkillRetrievalService;
    private final SkillSuggestionLlmClient skillSuggestionLlmClient;

    public AiSkillSuggestionResponse suggestSkillsForJobDescription(
            String jobDescriptionText,
            String jobTitle,
            String level,
            Integer experienceYears) {
        log.info("Generating AI skill suggestions for job description");
        List<Skill> candidates = candidateSkillRetrievalService.getCandidateSkills(jobDescriptionText);
        log.info("Retrieved {} candidate skills. Sending to LLM...", candidates.size());

        if (candidates.isEmpty()) {
            log.warn("No candidate skills available — skipping LLM call and returning empty suggestions");
            return new AiSkillSuggestionResponse(new ArrayList<>(), new ArrayList<>());
        }

        return skillSuggestionLlmClient.suggestSkills(jobDescriptionText, jobTitle, level, experienceYears, candidates);
    }
}
