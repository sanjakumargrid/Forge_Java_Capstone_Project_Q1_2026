package com.talentgrid.workforce.rmgdashboard.mapper;

import com.talentgrid.workforce.rmgdashboard.mapper.DemandStatusMapper.MappedTransition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DemandStatusMapperTest {

    @Test
    void mapsFilledInternalToDemandServiceFilled() {
        MappedTransition mapped = DemandStatusMapper.toDemandService("FILLED_INTERNAL", "FILLED_INTERNAL");

        assertEquals("FILLED", mapped.targetStatus());
        assertEquals("FILLED", mapped.closureReason());
    }

    @Test
    void mapsCancelledToClosedWithdrawn() {
        MappedTransition mapped = DemandStatusMapper.toDemandService("CANCELLED", null);

        assertEquals("CLOSED", mapped.targetStatus());
        assertEquals("WITHDRAWN", mapped.closureReason());
    }

    @Test
    void mapsOpenExternalWithNoInternalMatch() {
        MappedTransition mapped = DemandStatusMapper.toDemandService("OPEN_EXTERNAL", "NO_INTERNAL_MATCH");

        assertEquals("OPEN_EXTERNAL", mapped.targetStatus());
        assertEquals("NO_INTERNAL_MATCH", mapped.closureReason());
    }

    @Test
    void allowsOpenExternalWithoutClosureReason() {
        MappedTransition mapped = DemandStatusMapper.toDemandService("OPEN_EXTERNAL", null);

        assertEquals("OPEN_EXTERNAL", mapped.targetStatus());
        assertNull(mapped.closureReason());
    }

    @Test
    void rejectsOpenExternalWithFilledInternalClosureReason() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> DemandStatusMapper.toDemandService("OPEN_EXTERNAL", "FILLED_INTERNAL")
        );

        assertEquals(
                "Invalid closureReason for OPEN_EXTERNAL: 'FILLED'. Use NO_INTERNAL_MATCH, "
                        + "HM_REJECTED_NOMINATION, or omit closureReason after the internal-search gate has elapsed",
                ex.getMessage()
        );
    }

    @Test
    void passesThroughApprovedAndInternalSearch() {
        MappedTransition approved = DemandStatusMapper.toDemandService("APPROVED", null);
        MappedTransition internalSearch = DemandStatusMapper.toDemandService("INTERNAL_SEARCH", null);

        assertEquals("APPROVED", approved.targetStatus());
        assertNull(approved.closureReason());
        assertEquals("INTERNAL_SEARCH", internalSearch.targetStatus());
        assertNull(internalSearch.closureReason());
    }
}
