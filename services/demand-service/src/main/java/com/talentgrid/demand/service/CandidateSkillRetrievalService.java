package com.talentgrid.demand.service;

import com.talentgrid.demand.ai.AllAiProvidersFailedException;
import com.talentgrid.demand.config.EmbeddingDimensionConfig;
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
    private final EmbeddingDimensionConfig embeddingDimensionConfig;
    private final SkillEmbeddingBackfillAsyncService skillEmbeddingBackfillAsyncService;

    public List<Skill> getCandidateSkills(String jobDescriptionText) {
        SkillSuggestionMode mode = configService.getCurrentMode();

        if (mode == SkillSuggestionMode.FULL_CATALOG) {
            log.info("Using FULL_CATALOG mode for candidate retrieval.");
            return skillRepository.findAllByOrderBySkillNameAsc();
        } else {
            Integer topN = configService.getCurrentTopN();
            log.info("Using TOP_N_SIMILARITY mode for candidate retrieval with TopN={}", topN);

            try {
                float[] jdEmbedding = jdEmbeddingService.embedWithCache(jobDescriptionText);
                log.info("JD embedding for similarity search: textLength={}, dimension={}, expected={}",
                        jobDescriptionText.length(), jdEmbedding.length,
                        embeddingDimensionConfig.getExpectedDimension());
                embeddingDimensionConfig.validate(jdEmbedding, "JD similarity search");
                String vectorStr = formatVector(jdEmbedding);
                List<Skill> nearest = skillRepository.findNearestSkills(vectorStr, topN);
                skillEmbeddingBackfillAsyncService.backfillMissingInBackground();
                if (nearest.isEmpty()) {
                    long withEmbeddings = skillRepository.findAllByEmbeddingIsNotNull().size();
                    long total = skillRepository.count();
                    log.warn(
                            "TOP_N_SIMILARITY returned 0 skills (total={}, withEmbeddings={}); "
                                    + "falling back to FULL_CATALOG",
                            total, withEmbeddings);
                    return skillRepository.findAllByOrderBySkillNameAsc();
                }
                return nearest;
            } catch (AllAiProvidersFailedException e) {
                log.warn("Embedding unavailable in TOP_N_SIMILARITY mode; falling back to FULL_CATALOG: {}",
                        e.getMessage());
                return skillRepository.findAllByOrderBySkillNameAsc();
            } catch (IllegalStateException e) {
                log.warn("JD embedding dimension mismatch in TOP_N_SIMILARITY mode; falling back to FULL_CATALOG: {}",
                        e.getMessage());
                return skillRepository.findAllByOrderBySkillNameAsc();
            }
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
