package com.talentgrid.workforce.skillgapheatmap.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SkillTrendDto {

    private String skillName;
    private int previousGap;
    private int currentGap;
    private String trendDirection;
}
