package com.talentgrid.workforce.skillgapheatmap.provider.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Internal model representing a single open demand with its mandatory skills.
 * Built from demand-service list summaries (GET /api/v1/demands).
 */
@Data
@Builder
public class DemandResponse {

    private Long demandId;
    private String demandTitle;
    private Integer headcount;
    private List<String> requiredSkills;
}
