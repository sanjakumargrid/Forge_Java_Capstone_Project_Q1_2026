package com.talentgrid.demand.service;

import com.talentgrid.demand.dto.request.*;
import com.talentgrid.demand.dto.response.DemandResponse;
import org.springframework.stereotype.Service;

@Service
public class DemandLifecycleService {
    public DemandResponse submit(Long id, SubmitDemandRequest request) {
        return new DemandResponse();
    }

    public DemandResponse approve(Long id, ApproveDemandRequest request) {
        return new DemandResponse();
    }

    public DemandResponse hold(Long id, HoldDemandRequest request) {
        return new DemandResponse();
    }

    public DemandResponse cancel(Long id, CancelDemandRequest request) {
        return new DemandResponse();
    }
}
