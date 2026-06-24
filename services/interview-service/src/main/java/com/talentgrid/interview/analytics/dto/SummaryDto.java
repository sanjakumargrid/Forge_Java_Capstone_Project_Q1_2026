package com.talentgrid.interview.analytics.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SummaryDto {

    @JsonProperty("total_applications")
    private long totalApplications;

    @JsonProperty("offer_acceptance_rate_pct")
    private BigDecimal offerAcceptanceRatePct;
}