package com.talentgrid.demand.service;

import com.talentgrid.demand.domain.entity.Demand;
import com.talentgrid.demand.domain.enums.DemandPriority;
import com.talentgrid.demand.domain.enums.DemandStatus;
import com.talentgrid.demand.domain.enums.FillType;
import com.talentgrid.demand.domain.enums.SeniorityLevel;
import com.talentgrid.demand.dto.response.DemandPipelineResponse;
import com.talentgrid.demand.exception.DemandNotFoundException;
import com.talentgrid.demand.mapper.DemandMapper;
import com.talentgrid.demand.repository.DemandRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DemandQueryService#getPipeline(Long)} verifying that
 * the pipeline response carries {@code isFilled} / {@code fillType} correctly
 * in the single-person demand model.
 */
class DemandQueryServicePipelineTest {

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
    void getPipeline_isFilledFalseAndFillTypeNullWhenNotFilled() {
        Demand demand = buildDemand(1L, false, null, DemandStatus.INTERNAL_SEARCH);
        when(demandRepository.findByDemandIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(demand));

        DemandPipelineResponse pipeline = queryService.getPipeline(1L);

        assertEquals(1L, pipeline.getDemandId());
        assertFalse(pipeline.getIsFilled(), "isFilled must be false for an open demand");
        assertNull(pipeline.getFillType(), "fillType must be null when demand is not filled");
    }

    @Test
    void getPipeline_isFilledTrueWithInternalFillType() {
        Demand demand = buildDemand(2L, true, FillType.INTERNAL, DemandStatus.CLOSED);
        when(demandRepository.findByDemandIdAndIsDeletedFalse(2L)).thenReturn(Optional.of(demand));

        DemandPipelineResponse pipeline = queryService.getPipeline(2L);

        assertTrue(pipeline.getIsFilled());
        assertEquals("INTERNAL", pipeline.getFillType());
    }

    @Test
    void getPipeline_isFilledTrueWithExternalFillType() {
        Demand demand = buildDemand(3L, true, FillType.EXTERNAL, DemandStatus.CLOSED);
        when(demandRepository.findByDemandIdAndIsDeletedFalse(3L)).thenReturn(Optional.of(demand));

        DemandPipelineResponse pipeline = queryService.getPipeline(3L);

        assertTrue(pipeline.getIsFilled());
        assertEquals("EXTERNAL", pipeline.getFillType());
    }

    @Test
    void getPipeline_headcountFieldsDoNotExist() {
        Demand demand = buildDemand(4L, false, null, DemandStatus.DRAFT);
        when(demandRepository.findByDemandIdAndIsDeletedFalse(4L)).thenReturn(Optional.of(demand));

        DemandPipelineResponse pipeline = queryService.getPipeline(4L);

        // Verify the removed fields are absent from DemandPipelineResponse
        assertNoMethod(pipeline.getClass(), "getRequiredCount");
        assertNoMethod(pipeline.getClass(), "getRecruitedCount");
        assertNoMethod(pipeline.getClass(), "getInternalFilledCount");
        assertNoMethod(pipeline.getClass(), "getExternalFilledCount");
        assertNoMethod(pipeline.getClass(), "getRemainingCount");
    }

    @Test
    void getPipeline_throwsWhenDemandNotFound() {
        when(demandRepository.findByDemandIdAndIsDeletedFalse(99L)).thenReturn(Optional.empty());

        assertThrows(DemandNotFoundException.class, () -> queryService.getPipeline(99L));
    }

    @Test
    void getPipeline_statusIsPopulated() {
        Demand demand = buildDemand(5L, false, null, DemandStatus.OPEN_EXTERNAL);
        when(demandRepository.findByDemandIdAndIsDeletedFalse(5L)).thenReturn(Optional.of(demand));

        DemandPipelineResponse pipeline = queryService.getPipeline(5L);

        assertEquals("OPEN_EXTERNAL", pipeline.getStatus());
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Demand buildDemand(Long id, boolean isFilled, FillType fillType, DemandStatus status) {
        Demand demand = new Demand();
        demand.setDemandId(id);
        demand.setTitle("Backend Engineer");
        demand.setStatus(status);
        demand.setIsFilled(isFilled);
        demand.setFillType(fillType);
        demand.setIsDeleted(false);
        demand.setLevel(SeniorityLevel.T2_MID);
        demand.setPriority(DemandPriority.HIGH);
        demand.setLocation("Remote");
        demand.setBusinessUnit("Platform");
        return demand;
    }

    private void assertNoMethod(Class<?> clazz, String methodName) {
        try {
            clazz.getMethod(methodName);
            fail("Method " + methodName + "() should NOT exist on " + clazz.getSimpleName());
        } catch (NoSuchMethodException expected) {
            // Correct
        }
    }
}
