package com.talentgrid.demand.service;

import com.talentgrid.demand.domain.entity.Demand;
import com.talentgrid.demand.domain.enums.DemandStatus;
import com.talentgrid.demand.dto.response.DemandSummaryResponse;
import com.talentgrid.demand.mapper.DemandMapper;
import com.talentgrid.demand.repository.DemandRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DemandQueryService#searchDemands} multi-status filtering.
 */
class DemandQueryServiceSearchTest {

    @Mock
    private DemandRepository demandRepository;

    @Mock
    private DemandMapper demandMapper;

    @InjectMocks
    private DemandQueryService queryService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void searchDemands_withMultipleStatuses_delegatesToRepositoryWithSpecification() {
        Demand demand = new Demand();
        demand.setDemandId(1L);
        demand.setStatus(DemandStatus.APPROVED);
        demand.setIsDeleted(false);

        when(demandRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(demand)));
        when(demandMapper.toSummaryResponse(demand)).thenReturn(new DemandSummaryResponse());

        List<DemandStatus> statuses = List.of(DemandStatus.APPROVED, DemandStatus.INTERNAL_SEARCH);
        Page<DemandSummaryResponse> result = queryService.searchDemands(
                statuses, null, null, null, null, null,
                "createdAt", "desc", 0, 20);

        assertEquals(1, result.getTotalElements());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Specification<Demand>> specCaptor = ArgumentCaptor.forClass(Specification.class);
        verify(demandRepository).findAll(specCaptor.capture(), any(Pageable.class));
    }

    @Test
    void searchDemands_withNullStatuses_stillQueriesRepository() {
        when(demandRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        Page<DemandSummaryResponse> result = queryService.searchDemands(
                null, null, null, null, null, null,
                "createdAt", "desc", 0, 20);

        assertEquals(0, result.getTotalElements());
        verify(demandRepository).findAll(any(Specification.class), any(Pageable.class));
    }
}
