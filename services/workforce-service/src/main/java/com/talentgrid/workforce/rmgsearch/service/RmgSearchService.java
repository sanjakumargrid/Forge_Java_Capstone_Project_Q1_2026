package com.talentgrid.workforce.rmgsearch.service;

import com.talentgrid.workforce.rmgsearch.dto.RmgSearchRequest;
import com.talentgrid.workforce.rmgsearch.dto.RmgSearchResponse;

public interface RmgSearchService {

    /**
     * Search for bench employees using multi-criteria AND logic.
     * Filters by any combination of skill, availabilityDate range, location,
     * seniority, and contractType. Results are ranked by availabilityDate ascending.
     *
     * @param request the filter criteria
     * @return matching employees sorted by earliest availability first
     */
    RmgSearchResponse search(RmgSearchRequest request);
}
