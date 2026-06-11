package com.talentgrid.workforce.skillgapheatmap.provider.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * Mirrors the fields from demand-service's DemandResponse
 * (GET /api/v1/demands/{id}) that are relevant to the skill gap heatmap.
 * Kept as a local DTO so the workforce-service has no compile-time
 * dependency on the demand-service module.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DemandServiceResponse {

    private Long demandId;
    private String title;
    private String status;
    private Integer requiredCount;
    private List<String> skills;
}
