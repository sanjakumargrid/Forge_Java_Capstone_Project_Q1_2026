package com.talentgrid.demand.service;

import com.talentgrid.demand.domain.entity.Skill;
import com.talentgrid.demand.domain.enums.SkillSuggestionMode;
import com.talentgrid.demand.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CandidateSkillRetrievalService {

    private final AiSkillSuggestionConfigService configService;
    private final SkillRepository skillRepository;
    private final JdEmbeddingService jdEmbeddingService;

    public List<Skill> getCandidateSkills(String jobDescriptionText) {
        SkillSuggestionMode mode = configService.getCurrentMode();
        
        if (mode == SkillSuggestionMode.FULL_CATALOG) {
            log.info("Using FULL_CATALOG mode for candidate retrieval.");
            return skillRepository.findAllByOrderBySkillNameAsc();
        } else {
            Integer topN = configService.getCurrentTopN();
            log.info("Using TOP_N_SIMILARITY mode for candidate retrieval with TopN={}", topN);
            
            float[] jdEmbedding = jdEmbeddingService.embedWithCache(jobDescriptionText);
            String vectorStr = formatVector(jdEmbedding);
            
            return skillRepository.findNearestSkills(vectorStr, topN);
        }
    }

    private String formatVector(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            sb.append(vector[i]);
            if (i < vector.length - 1) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
