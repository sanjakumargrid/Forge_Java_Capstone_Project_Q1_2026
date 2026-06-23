package com.talentgrid.demand.service;

import com.talentgrid.demand.domain.entity.AiSkillSuggestionConfig;
import com.talentgrid.demand.domain.enums.SkillSuggestionMode;
import com.talentgrid.demand.repository.AiSkillSuggestionConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiSkillSuggestionConfigService {

    private final AiSkillSuggestionConfigRepository configRepository;

    public AiSkillSuggestionConfig getCurrentConfig() {
        return configRepository.findTopByOrderByIdDesc()
                .orElseGet(() -> {
                    AiSkillSuggestionConfig defaultConfig = new AiSkillSuggestionConfig();
                    defaultConfig.setMode(SkillSuggestionMode.TOP_N_SIMILARITY);
                    defaultConfig.setTopN(40);
                    return defaultConfig;
                });
    }

    public SkillSuggestionMode getCurrentMode() {
        return getCurrentConfig().getMode();
    }

    public Integer getCurrentTopN() {
        return getCurrentConfig().getTopN();
    }

    @Transactional
    public AiSkillSuggestionConfig updateConfig(SkillSuggestionMode mode, Integer topN, Long updatedBy) {
        AiSkillSuggestionConfig newConfig = new AiSkillSuggestionConfig();
        newConfig.setMode(mode);
        newConfig.setTopN(topN);
        newConfig.setUpdatedBy(updatedBy);
        return configRepository.save(newConfig);
    }
}
