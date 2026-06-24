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
public class AllocationRateResponse {

    /** Average {@code utilisationPct} across active internal employees (0–100 scale). */
    private BigDecimal allocationRatePercent;
}
