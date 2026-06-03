package com.talentgrid.demand.service;

import com.talentgrid.demand.dto.response.DemandAnalyticsResponse;
import org.springframework.stereotype.Service;

@Service
public class DemandAnalyticsService {
    public DemandAnalyticsResponse getAnalytics() {
        return new DemandAnalyticsResponse();
    }
}
