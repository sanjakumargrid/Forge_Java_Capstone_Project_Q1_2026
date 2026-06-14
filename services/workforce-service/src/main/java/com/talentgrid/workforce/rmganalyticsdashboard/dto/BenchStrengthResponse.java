package com.talentgrid.workforce.rmganalyticsdashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BenchStrengthResponse {

    /** Sum of engineers in the three availability windows (same source as bench report). */
    private int totalBench;

    private BenchStrengthWindowsDto byWindow;
}
