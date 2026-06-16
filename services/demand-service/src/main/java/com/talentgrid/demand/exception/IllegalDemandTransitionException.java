package com.talentgrid.demand.exception;

/**
 * Exception thrown when an invalid status transition is attempted, violating the
 * state machine rules (e.g., trying to move directly from DRAFT to FILLED).
 * Resolves to HTTP 409 CONFLICT via the global exception handler.
 */
public class IllegalDemandTransitionException extends DemandServiceException {
    public IllegalDemandTransitionException(String message) {
        super(message);
    }
}
