package com.talentgrid.workforce.skillgapheatmap.provider;

import com.talentgrid.workforce.skillgapheatmap.provider.model.DemandResponse;

import java.util.List;

/**
 * Port interface for fetching open demands with their required skills.
 * Production implementation: FeignDemandProvider (calls demand-service via HTTP).
 */
public interface DemandProvider {

    List<DemandResponse> getOpenDemands();
}
