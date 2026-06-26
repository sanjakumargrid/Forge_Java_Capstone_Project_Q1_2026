package com.talentgrid.demand.service;

import com.talentgrid.demand.domain.entity.Demand;
import com.talentgrid.demand.domain.enums.*;
import com.talentgrid.demand.dto.request.DemandRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link DemandValidationService} — verifying that
 * {@code requiredCount} is no longer validated (single-person demand model).
 */
class DemandValidationServiceFillTest {

    @InjectMocks
    private DemandValidationService validationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // ─── validateCreate ───────────────────────────────────────────────────────

    @Test
    void validateCreate_succeedsWithoutRequiredCount() {
        // In the old model, omitting requiredCount caused a validation failure.
        // After the single-person refactor, no headcount field is needed.
        DemandRequest request = validCreateRequest();
        assertDoesNotThrow(() -> validationService.validateCreate(request),
                "Create validation must succeed without a requiredCount field");
    }

    @Test
    void validateCreate_failsWhenLevelMissing() {
        DemandRequest request = validCreateRequest();
        request.setLevel(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> validationService.validateCreate(request));
        assertTrue(ex.getMessage().contains("level"), "Error must mention 'level'");
    }

    @Test
    void validateCreate_failsWhenPriorityMissing() {
        DemandRequest request = validCreateRequest();
        request.setPriority(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> validationService.validateCreate(request));
        assertTrue(ex.getMessage().contains("priority"), "Error must mention 'priority'");
    }

    @Test
    void validateCreate_failsWhenLocationMissing() {
        DemandRequest request = validCreateRequest();
        request.setLocation(null);

        assertThrows(IllegalArgumentException.class,
                () -> validationService.validateCreate(request));
    }

    @Test
    void validateCreate_failsWhenBudgetIsNegative() {
        DemandRequest request = validCreateRequest();
        request.setBudget(BigDecimal.valueOf(-1));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> validationService.validateCreate(request));
        assertTrue(ex.getMessage().contains("budget"), "Error must mention 'budget'");
    }

    @Test
    void validateCreate_failsWhenNoSkillsProvided() {
        DemandRequest request = validCreateRequest();
        request.setMandatorySkillIds(null);
        request.setOptionalSkillIds(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> validationService.validateCreate(request));
        assertTrue(ex.getMessage().contains("mandatorySkillIds") || ex.getMessage().contains("optionalSkillIds"),
                "Error must mention skill fields");
    }

    // ─── validateUpdate ───────────────────────────────────────────────────────

    @Test
    void validateUpdate_emptyRequestPassesWithoutErrors() {
        // A fully null update is a no-op — should not throw
        assertDoesNotThrow(() -> validationService.validateUpdate(new DemandRequest()));
    }

    @Test
    void validateUpdate_doesNotRejectAbsenceOfRequiredCount() {
        // requiredCount removal: any update that doesn't specify requiredCount is fine
        DemandRequest request = new DemandRequest();
        request.setLocation("Berlin");

        assertDoesNotThrow(() -> validationService.validateUpdate(request),
                "Update validation must not require requiredCount");
    }

    @Test
    void validateUpdate_failsOnNegativeBudget() {
        DemandRequest request = new DemandRequest();
        request.setBudget(BigDecimal.valueOf(-500));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> validationService.validateUpdate(request));
        assertTrue(ex.getMessage().contains("budget"));
    }

    @Test
    void validateUpdate_failsWhenSkillsUpdatedButBothEmpty() {
        DemandRequest request = new DemandRequest();
        request.setMandatorySkillIds(java.util.List.of());
        request.setOptionalSkillIds(java.util.List.of());

        assertThrows(IllegalArgumentException.class,
                () -> validationService.validateUpdate(request));
    }

    @Test
    void validateCreate_failsWhenSkillAppearsInBothLists() {
        DemandRequest request = validCreateRequest();
        request.setMandatorySkillIds(java.util.List.of(1L, 2L));
        request.setOptionalSkillIds(java.util.List.of(2L, 3L));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> validationService.validateCreate(request));
        assertTrue(ex.getMessage().contains("both mandatorySkillIds and optionalSkillIds"));
    }

    @Test
    void validateCreate_failsWhenMandatorySkillsContainDuplicates() {
        DemandRequest request = validCreateRequest();
        request.setMandatorySkillIds(java.util.List.of(1L, 1L));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> validationService.validateCreate(request));
        assertTrue(ex.getMessage().contains("duplicate skill IDs"));
    }

    @Test
    void validateSkillLists_failsWhenMergedListsOverlap() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> validationService.validateSkillLists(
                        java.util.List.of(1L),
                        java.util.List.of(1L, 2L)));
        assertTrue(ex.getMessage().contains("both mandatorySkillIds and optionalSkillIds"));
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private DemandRequest validCreateRequest() {
        DemandRequest req = new DemandRequest();
        req.setLevel(SeniorityLevel.T2_MID);
        req.setPriority(DemandPriority.MEDIUM);
        req.setLocation("Remote");
        req.setBusinessUnit("Engineering");
        req.setDepartment("Engineering");
        req.setBudget(BigDecimal.valueOf(90_000));
        req.setTargetDate(java.time.LocalDate.now().plusMonths(2));
        req.setJobTitleId(10L);
        req.setProjectId(1L);
        req.setMandatorySkillIds(java.util.List.of(1L, 2L));
        return req;
    }
}
