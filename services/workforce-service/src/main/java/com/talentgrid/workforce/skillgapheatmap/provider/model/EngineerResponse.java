package com.talentgrid.workforce.skillgapheatmap.provider.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Internal model representing a bench engineer with their current skills.
 * Built from BenchEmployeeDto by BenchReportWorkforceProvider.
 */
@Data
@Builder
public class EngineerResponse {

    private String employeeId;
    private String name;
    private List<String> skills;
}
