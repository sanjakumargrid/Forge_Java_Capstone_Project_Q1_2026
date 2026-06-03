package com.talentgrid.demand.service;

import com.talentgrid.demand.dto.response.DemandResponse;
import com.talentgrid.demand.dto.response.DemandSummaryResponse;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class DemandQueryService {
    public DemandResponse getDemandById(Long id) {
        return new DemandResponse();
    }

    public List<DemandSummaryResponse> getAllDemands() {
        return Collections.emptyList();
    }
}
