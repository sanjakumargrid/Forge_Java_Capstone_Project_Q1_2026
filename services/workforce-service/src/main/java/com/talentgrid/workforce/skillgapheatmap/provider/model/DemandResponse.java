package com.talentgrid.workforce.skillgapheatmap.provider.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Internal model representing a single open demand with its required skills.
 * Built from DemandServiceResponse after filtering by open status.
 */
@Data
@Builder
public class DemandResponse {

    private Long demandId;
    private String demandTitle;
    private Integer headcount;
    private List<String> requiredSkills;
}
