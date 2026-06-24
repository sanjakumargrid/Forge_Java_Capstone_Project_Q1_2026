package com.talentgrid.interview.analytics.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalyticsResponse {

    @JsonProperty("summary")
    private SummaryDto summary;

    @JsonProperty("pipeline_conversion_rates")
    private List<ConversionRateDto> pipelineConversionRates;

    @JsonProperty("avg_time_per_stage_days")
    private Map<String, Double> avgTimePerStageDays;
}