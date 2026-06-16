package com.talentgrid.demand.exception;

/**
 * Exception thrown when a requested Demand cannot be found in the database,
 * or when it has been soft-deleted.
 * Resolves to HTTP 404 NOT FOUND via the global exception handler.
 */
public class DemandNotFoundException extends DemandServiceException {
    public DemandNotFoundException(String message) {
        super(message);
    }
}
