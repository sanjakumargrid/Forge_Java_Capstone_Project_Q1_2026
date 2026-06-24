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
public class BenchDistributionByAvailabilityResponse {

    private int totalBenchEmployees;

    @Builder.Default
    private List<BenchAvailabilityWindowCountDto> windows = new ArrayList<>();
}
