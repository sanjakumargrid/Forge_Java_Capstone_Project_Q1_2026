package com.talentgrid.workforce.benchreport.service;

import com.talentgrid.workforce.benchreport.dto.BenchReportPageResponse;
import com.talentgrid.workforce.benchreport.dto.BenchReportResponse;

public interface BenchReportService {

    BenchReportResponse getBenchReport();

    BenchReportPageResponse getBenchReport(int page, int size);

    void refreshBenchReport();
}
