package com.talentgrid.demand.exception;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;

import com.talentgrid.demand.dto.response.ErrorResponse;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DemandExceptionHandlerTest {

    private final DemandExceptionHandler handler = new DemandExceptionHandler();

    @Test
    void handleDataIntegrityViolation_notNullViolation_returnsSpecificMessage() {
        SQLException sqlException = new SQLException(
                "ERROR: null value in column \"comments\" of relation \"demand_status_history\" "
                        + "violates not-null constraint",
                "23502");
        DataIntegrityViolationException ex = new DataIntegrityViolationException("insert failed", sqlException);

        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrityViolation(ex);

        assertEquals(400, response.getStatusCode().value());
        assertTrue(response.getBody().getMessage().contains("comments"));
        assertTrue(response.getBody().getMessage().contains("demand_status_history"));
    }

    @Test
    void handleDataIntegrityViolation_uniqueViolation_returnsDuplicateMessage() {
        SQLException sqlException = new SQLException(
                "ERROR: duplicate key value violates unique constraint \"uq_demand_title\"",
                "23505");
        DataIntegrityViolationException ex = new DataIntegrityViolationException("insert failed", sqlException);

        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrityViolation(ex);

        assertEquals(400, response.getStatusCode().value());
        assertTrue(response.getBody().getMessage().contains("Duplicate"));
        assertTrue(response.getBody().getMessage().contains("uq_demand_title"));
    }

    @Test
    void handleDataIntegrityViolation_demandSkillUnique_returnsSpecificMessage() {
        SQLException sqlException = new SQLException(
                "ERROR: duplicate key value violates unique constraint \"uq_demand_skill\"",
                "23505");
        DataIntegrityViolationException ex = new DataIntegrityViolationException("insert failed", sqlException);

        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrityViolation(ex);

        assertEquals(400, response.getStatusCode().value());
        assertTrue(response.getBody().getMessage().contains("Duplicate skill association"));
    }

    @Test
    void handleDataIntegrityViolation_foreignKeyViolation_returnsReferenceMessage() {
        SQLException sqlException = new SQLException(
                "ERROR: insert or update on table \"demand\" violates foreign key constraint \"fk_demand_project\"",
                "23503");
        DataIntegrityViolationException ex = new DataIntegrityViolationException("insert failed", sqlException);

        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrityViolation(ex);

        assertEquals(400, response.getStatusCode().value());
        assertTrue(response.getBody().getMessage().contains("Referenced record"));
        assertTrue(response.getBody().getMessage().contains("fk_demand_project"));
    }
}
