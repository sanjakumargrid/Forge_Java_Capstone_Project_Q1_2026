package com.talentgrid.workforce.skillgapheatmap.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class SkillTrendResponse {

    private LocalDateTime generatedAt;
    private List<SkillTrendDto> trends;
    private boolean degraded;
    private String degradedReason;
}
