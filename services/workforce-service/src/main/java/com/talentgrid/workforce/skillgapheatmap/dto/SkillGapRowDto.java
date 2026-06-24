package com.talentgrid.workforce.skillgapheatmap.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SkillGapRowDto {

    private String skillName;
    private int demandCount;
    private int benchCount;
    private int gapScore;
    private String gapLevel;
    private String trendDirection;
}
