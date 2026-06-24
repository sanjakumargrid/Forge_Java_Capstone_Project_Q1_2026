package com.talentgrid.workforce.rmganalyticsdashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BenchDistributionBySkillResponse {

    /** Bench employees counted (COALESCE(utilisation_pct,0) = 0, not deleted). */
    private int totalBenchEmployees;

    /** Top skills by occurrence, sorted descending by count. */
    @Builder.Default
    private List<BenchSkillCountDto> skills = new ArrayList<>();
}
