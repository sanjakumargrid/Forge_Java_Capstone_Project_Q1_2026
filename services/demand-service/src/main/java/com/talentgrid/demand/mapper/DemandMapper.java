package com.talentgrid.demand.mapper;

import com.talentgrid.demand.domain.entity.Demand;
import com.talentgrid.demand.dto.response.DemandResponse;
import com.talentgrid.demand.dto.response.DemandSummaryResponse;
import org.springframework.stereotype.Component;

@Component
public class DemandMapper {
    public DemandResponse toResponse(Demand demand) {
        if (demand == null) return null;
        DemandResponse response = new DemandResponse();
        response.setId(demand.getId());
        response.setTitle(demand.getTitle());
        response.setStatus(demand.getStatus() != null ? demand.getStatus().name() : null);
        return response;
    }

    public DemandSummaryResponse toSummaryResponse(Demand demand) {
        if (demand == null) return null;
        DemandSummaryResponse response = new DemandSummaryResponse();
        response.setId(demand.getId());
        response.setTitle(demand.getTitle());
        response.setStatus(demand.getStatus() != null ? demand.getStatus().name() : null);
        return response;
    }
}
