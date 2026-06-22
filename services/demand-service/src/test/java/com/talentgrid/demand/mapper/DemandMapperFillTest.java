package com.talentgrid.demand.mapper;

import com.talentgrid.demand.domain.entity.Demand;
import com.talentgrid.demand.domain.enums.*;
import com.talentgrid.demand.dto.request.DemandRequest;
import com.talentgrid.demand.dto.response.DemandResponse;
import com.talentgrid.demand.dto.response.DemandSummaryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link DemandMapper} — verifying the single-person fill model:
 * isFilled / fillType replace the removed headcount fields.
 */
class DemandMapperFillTest {

    private DemandMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new DemandMapper();
    }

    // ─── toEntity ─────────────────────────────────────────────────────────────

    @Test
    void toEntity_defaultsIsFilledToFalse() {
        DemandRequest request = minimalRequest();
        Demand demand = mapper.toEntity(request);

        assertFalse(demand.getIsFilled(),
                "Newly created demand must default isFilled to false");
    }

    @Test
    void toEntity_fillTypeIsNullByDefault() {
        DemandRequest request = minimalRequest();
        Demand demand = mapper.toEntity(request);

        assertNull(demand.getFillType(),
                "fillType must be null for a freshly created demand");
    }

    @Test
    void toEntity_statusIsDraftByDefault() {
        Demand demand = mapper.toEntity(minimalRequest());
        assertEquals(DemandStatus.DRAFT, demand.getStatus());
    }

    @Test
    void toEntity_doesNotAcceptRequiredCount() {
        // DemandRequest must NOT have a requiredCount field any more.
        // Verify the entity has no requiredCount either.
        Demand demand = mapper.toEntity(minimalRequest());
        // If the field still existed, this would be a compile error — defensive runtime check:
        try {
            demand.getClass().getMethod("getRequiredCount");
            fail("getRequiredCount() should not exist on Demand after headcount removal");
        } catch (NoSuchMethodException expected) {
            // Correct — field has been removed
        }
    }

    // ─── toResponse ───────────────────────────────────────────────────────────

    @Test
    void toResponse_isFilledFalseWhenNotFilled() {
        Demand demand = buildDemand(false, null);
        DemandResponse response = mapper.toResponse(demand);

        assertFalse(response.getIsFilled());
        assertNull(response.getFillType(), "fillType must be null when demand is not filled");
    }

    @Test
    void toResponse_isFilledTrueWithInternalFillType() {
        Demand demand = buildDemand(true, FillType.INTERNAL);
        DemandResponse response = mapper.toResponse(demand);

        assertTrue(response.getIsFilled());
        assertEquals("INTERNAL", response.getFillType());
    }

    @Test
    void toResponse_isFilledTrueWithExternalFillType() {
        Demand demand = buildDemand(true, FillType.EXTERNAL);
        DemandResponse response = mapper.toResponse(demand);

        assertTrue(response.getIsFilled());
        assertEquals("EXTERNAL", response.getFillType());
    }

    @Test
    void toResponse_headcountFieldsDoNotExist() {
        DemandResponse response = mapper.toResponse(buildDemand(false, null));
        // These getter names must NOT exist on DemandResponse
        assertNoMethod(response.getClass(), "getRequiredCount");
        assertNoMethod(response.getClass(), "getRecruitedCount");
        assertNoMethod(response.getClass(), "getInternalFilledCount");
        assertNoMethod(response.getClass(), "getExternalFilledCount");
    }

    // ─── toSummaryResponse ────────────────────────────────────────────────────

    @Test
    void toSummaryResponse_isFilledFalseWhenNotFilled() {
        Demand demand = buildDemand(false, null);
        DemandSummaryResponse summary = mapper.toSummaryResponse(demand);

        assertFalse(summary.getIsFilled());
    }

    @Test
    void toSummaryResponse_isFilledTrueForFilledDemand() {
        Demand demand = buildDemand(true, FillType.INTERNAL);
        DemandSummaryResponse summary = mapper.toSummaryResponse(demand);

        assertTrue(summary.getIsFilled());
    }

    @Test
    void toSummaryResponse_headcountFieldsDoNotExist() {
        DemandSummaryResponse summary = mapper.toSummaryResponse(buildDemand(false, null));
        assertNoMethod(summary.getClass(), "getRequiredCount");
        assertNoMethod(summary.getClass(), "getInternalFilledCount");
        assertNoMethod(summary.getClass(), "getExternalFilledCount");
    }

    // ─── applyUpdate ──────────────────────────────────────────────────────────

    @Test
    void applyUpdate_doesNotResetIsFilled() {
        // Simulate a demand that is already filled
        Demand demand = buildDemand(true, FillType.EXTERNAL);

        // A PATCH update with unrelated fields should NOT reset isFilled
        DemandRequest patch = new DemandRequest();
        patch.setLocation("New York");
        mapper.applyUpdate(patch, demand);

        assertTrue(demand.getIsFilled(), "applyUpdate must not reset isFilled");
        assertEquals(FillType.EXTERNAL, demand.getFillType(), "applyUpdate must not reset fillType");
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private DemandRequest minimalRequest() {
        DemandRequest req = new DemandRequest();
        req.setLevel(SeniorityLevel.T2_MID);
        req.setPriority(DemandPriority.MEDIUM);
        req.setLocation("Remote");
        req.setBusinessUnit("Engineering");
        req.setBudget(BigDecimal.valueOf(100_000));
        return req;
    }

    private Demand buildDemand(boolean isFilled, FillType fillType) {
        Demand demand = new Demand();
        demand.setDemandId(1L);
        demand.setTitle("Senior Java Developer");
        demand.setStatus(isFilled ? DemandStatus.FILLED_INTERNAL : DemandStatus.INTERNAL_SEARCH);
        demand.setIsFilled(isFilled);
        demand.setFillType(fillType);
        demand.setIsDeleted(false);
        demand.setLevel(SeniorityLevel.T3_SENIOR);
        demand.setPriority(DemandPriority.HIGH);
        demand.setLocation("Remote");
        demand.setBusinessUnit("Engineering");
        return demand;
    }

    private void assertNoMethod(Class<?> clazz, String methodName) {
        try {
            clazz.getMethod(methodName);
            fail("Method " + methodName + "() should NOT exist on " + clazz.getSimpleName()
                    + " after headcount removal");
        } catch (NoSuchMethodException expected) {
            // Correct — the field has been removed
        }
    }
}
