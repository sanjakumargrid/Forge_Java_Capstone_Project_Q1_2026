package com.talentgrid.demand.exception;

/**
 * Base exception for all Demand Service domain exceptions.
 */
public abstract class DemandServiceException extends RuntimeException {
    public DemandServiceException(String message) {
        super(message);
    }

    public DemandServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
