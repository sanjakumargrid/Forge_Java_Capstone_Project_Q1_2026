package com.talentgrid.demand.exception;

/**
 * Exception thrown when an operation is attempted on a Demand that is not
 * in the correct state (e.g., trying to edit a Demand that is not in DRAFT status).
 * Resolves to HTTP 400 BAD REQUEST via the global exception handler.
 */
public class InvalidDemandStateException extends DemandServiceException {
    public InvalidDemandStateException(String message) {
        super(message);
    }
}
