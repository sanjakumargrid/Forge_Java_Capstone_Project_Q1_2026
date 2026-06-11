package com.talentgrid.workforce.skillgapheatmap.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SkillGapSummaryResponse {

    private int totalSkills;
    private int criticalSkills;
    private int highSkills;
    private int mediumSkills;
    private int lowSkills;
    private LocalDateTime generatedAt;
    private boolean degraded;
    private String degradedReason;
}
