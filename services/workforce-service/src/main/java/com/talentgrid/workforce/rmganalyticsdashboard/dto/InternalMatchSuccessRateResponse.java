package com.talentgrid.workforce.rmganalyticsdashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternalMatchSuccessRateResponse {

    /** Successful matches / decided matches, 0–100. Null if no decided matches in scope. */
    private BigDecimal rate;

    private long successfulMatches;
    private long totalDecidedMatches;
}
