package com.talentgrid.interview.analytics.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversionRateDto {

    @JsonProperty("stage")
    private String stage;

    @JsonProperty("count")
    private long count;

    @JsonProperty("conversion_from_previous_pct")
    private double conversionFromPreviousPct;

    @JsonProperty("drop-off_rate_pct")
    private double dropOffRatePct;
}
