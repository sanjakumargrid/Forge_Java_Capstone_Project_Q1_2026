package com.talentgrid.workforce.benchreport.dto;

import lombok.Data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
public class BenchReportResponse {

    private Instant refreshedAt;
    private List<BenchEmployeeDto> under30Days = new ArrayList<>();
    private List<BenchEmployeeDto> thirtyToSixtyDays = new ArrayList<>();
    private List<BenchEmployeeDto> sixtyToNinetyDays = new ArrayList<>();
}
