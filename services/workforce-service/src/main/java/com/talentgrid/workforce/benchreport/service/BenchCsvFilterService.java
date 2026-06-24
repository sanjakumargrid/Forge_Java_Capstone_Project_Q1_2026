package com.talentgrid.workforce.benchreport.service;

import com.talentgrid.workforce.benchreport.dto.BenchFilterPageResponse;
import com.talentgrid.workforce.benchreport.dto.BenchFilterRequest;

public interface BenchCsvFilterService {
    
    BenchFilterPageResponse searchBenchEmployees(BenchFilterRequest request, int page, int size, String sortBy, String sortDirection);
    
    byte[] exportBenchEmployeesCsv(BenchFilterRequest request, String sortBy, String sortDirection);
}
