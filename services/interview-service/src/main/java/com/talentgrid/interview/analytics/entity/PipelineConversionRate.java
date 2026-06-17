package com.talentgrid.interview.analytics.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PipelineConversionRate implements Serializable {

    private String stage;

    private Integer count;

    @JsonProperty("conversion_from_previous_pct")
    private Double conversionFromPreviousPct;

    @JsonProperty("drop-off_rate_pct")
    private Double dropOffRatePct;
}
