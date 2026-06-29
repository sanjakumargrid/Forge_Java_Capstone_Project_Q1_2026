package com.talentgrid.workforce.benchreport.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class BenchReportPageResponse {

    private Instant refreshedAt;
    private BenchWindowPage under30Days;
    private BenchWindowPage thirtyToSixtyDays;
    private BenchWindowPage sixtyToNinetyDays;
}
