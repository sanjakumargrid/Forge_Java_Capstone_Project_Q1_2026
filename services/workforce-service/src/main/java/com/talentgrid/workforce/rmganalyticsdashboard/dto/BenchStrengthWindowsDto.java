package com.talentgrid.workforce.rmganalyticsdashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BenchStrengthWindowsDto {

    private int under30Days;
    private int thirtyToSixtyDays;
    private int sixtyToNinetyDays;
}
