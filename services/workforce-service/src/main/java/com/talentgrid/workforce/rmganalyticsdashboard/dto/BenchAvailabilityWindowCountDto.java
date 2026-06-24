package com.talentgrid.workforce.rmganalyticsdashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BenchAvailabilityWindowCountDto {

    /** Stable key for charts, e.g. IMMEDIATE, WITHIN_TWO_WEEKS. */
    private String windowKey;

    /** Human-readable label for UI. */
    private String label;

    private int count;
}
