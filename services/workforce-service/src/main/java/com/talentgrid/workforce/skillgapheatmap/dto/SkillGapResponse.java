package com.talentgrid.workforce.skillgapheatmap.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class SkillGapResponse {

    private LocalDateTime generatedAt;
    private int totalSkills;
    private List<SkillGapRowDto> skills;
    private boolean degraded;
    private String degradedReason;
}
