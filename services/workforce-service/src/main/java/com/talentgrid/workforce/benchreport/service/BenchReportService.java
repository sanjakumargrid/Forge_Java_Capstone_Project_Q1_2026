package com.talentgrid.workforce.benchreport.service;

import com.talentgrid.workforce.benchreport.dto.BenchReportResponse;

public interface BenchReportService {

    BenchReportResponse getBenchReport();

    void refreshBenchReport();
}
