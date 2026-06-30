package com.talentgrid.workforce.rmgsearch.service;

import com.talentgrid.workforce.rmgsearch.dto.RmgSearchRequest;
import com.talentgrid.workforce.rmgsearch.dto.RmgSearchResponse;

public interface RmgSearchService {

    /**
     * Search for bench employees using multi-criteria AND logic across filters,
     * with OR logic when multiple skills are supplied. Results are ranked by
     * availabilityDate ascending and paginated.
     *
     * @param request the filter criteria
     * @param page    zero-based page index
     * @param size    page size
     * @return matching employees sorted by earliest availability first
     */
    RmgSearchResponse search(RmgSearchRequest request, int page, int size);
}
