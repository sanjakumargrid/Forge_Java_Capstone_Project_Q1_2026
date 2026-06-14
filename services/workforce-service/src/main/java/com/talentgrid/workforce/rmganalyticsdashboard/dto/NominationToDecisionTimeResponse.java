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
public class NominationToDecisionTimeResponse {

    /** Average days from nomination to review decision. Null when no qualifying rows. */
    private BigDecimal averageDays;
}
