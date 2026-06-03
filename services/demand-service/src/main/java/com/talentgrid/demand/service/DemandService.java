package com.talentgrid.demand.service;

import com.talentgrid.demand.dto.request.CreateDemandRequest;
import com.talentgrid.demand.dto.request.UpdateDemandRequest;
import com.talentgrid.demand.dto.response.DemandResponse;
import org.springframework.stereotype.Service;

@Service
public class DemandService {
    public DemandResponse createDemand(CreateDemandRequest request) {
        return new DemandResponse();
    }

    public DemandResponse updateDemand(Long id, UpdateDemandRequest request) {
        return new DemandResponse();
    }
}
