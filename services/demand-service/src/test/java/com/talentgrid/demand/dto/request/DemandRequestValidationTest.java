package com.talentgrid.demand.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DemandRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void blankDescription_failsWithRequiredMessage() {
        DemandRequest request = new DemandRequest();
        request.setDescription(null);

        Set<ConstraintViolation<DemandRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
        assertEquals("Description is required.", violations.iterator().next().getMessage());
    }

    @Test
    void descriptionShorterThan250_failsWithSizeMessage() {
        DemandRequest request = new DemandRequest();
        request.setDescription("a".repeat(249));

        Set<ConstraintViolation<DemandRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
        assertEquals("Description must be between 250 and 2000 characters.", violations.iterator().next().getMessage());
    }

    @Test
    void descriptionBetween250And2000_passesValidation() {
        DemandRequest request = new DemandRequest();
        request.setDescription("a".repeat(500));

        Set<ConstraintViolation<DemandRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    void descriptionLongerThan2000_failsWithSizeMessage() {
        DemandRequest request = new DemandRequest();
        request.setDescription("a".repeat(2001));

        Set<ConstraintViolation<DemandRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
        assertEquals("Description must be between 250 and 2000 characters.", violations.iterator().next().getMessage());
    }
}
