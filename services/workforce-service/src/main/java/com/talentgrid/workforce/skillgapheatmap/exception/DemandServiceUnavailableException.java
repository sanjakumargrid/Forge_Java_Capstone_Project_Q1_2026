package com.talentgrid.workforce.skillgapheatmap.exception;

/**
 * Raised when the demand-service cannot be reached or returns a server error,
 * so the heatmap must not be refreshed with incomplete demand data.
 */
public class DemandServiceUnavailableException extends RuntimeException {

    public DemandServiceUnavailableException(String message) {
        super(message);
    }

    public DemandServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
