package com.talentgrid.demand.controller;

import com.talentgrid.demand.domain.entity.AiSkillSuggestionConfig;
import com.talentgrid.demand.domain.enums.SkillSuggestionMode;
import com.talentgrid.demand.service.AiSkillSuggestionConfigService;
import com.talentgrid.demand.util.SecurityUtils;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/ai-skill-suggestion-config")
@RequiredArgsConstructor
public class AiSkillSuggestionAdminController {

    private final AiSkillSuggestionConfigService configService;

    @GetMapping
    @PreAuthorize("hasAuthority('AI_SKILL_CONFIG_MANAGE')")
    public AiSkillSuggestionConfig getConfig() {
        return configService.getCurrentConfig();
    }

    @PutMapping
    @PreAuthorize("hasAuthority('AI_SKILL_CONFIG_MANAGE')")
    public AiSkillSuggestionConfig updateConfig(
            @RequestParam SkillSuggestionMode mode,
            @RequestParam Integer topN) {
        return configService.updateConfig(mode, topN, SecurityUtils.getCurrentUserId());
    }


}
