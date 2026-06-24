package com.talentgrid.workforce.skillgapheatmap.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class RefreshResponse {

    private String status;
    private int processedSkills;
    private LocalDateTime refreshedAt;
}
