package com.talentgrid.auth.exception;

/**
 * Thrown when a requested domain resource cannot be found.
 *
 * <p>Mapped to HTTP 404 by {@link GlobalExceptionHandler}.
 */
public class ResourceNotFoundException extends RuntimeException {

    /**
     * Creates a not-found exception with a descriptive message.
     *
     * @param message human-readable explanation of the missing resource
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
