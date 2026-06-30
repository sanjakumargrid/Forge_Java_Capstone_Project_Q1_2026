package com.talentgrid.workforce.rmgdashboard.service;

import com.talentgrid.workforce.rmgdashboard.dto.DemandSearchRequest;
import com.talentgrid.workforce.rmgdashboard.dto.DemandSearchResponse;

public interface DemandSearchService {

    DemandSearchResponse search(DemandSearchRequest request);
}
