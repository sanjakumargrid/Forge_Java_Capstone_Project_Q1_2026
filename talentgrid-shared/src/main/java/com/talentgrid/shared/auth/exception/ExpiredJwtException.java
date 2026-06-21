package com.talentgrid.shared.auth.exception;

/**
 * Exception thrown when a JWT token's expiration date is in the past.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Signal that a previously valid token has naturally expired</li>
 *   <li>Allow consuming services to catch this specific exception and
 *       issue a proper 401 Unauthorized response (e.g., triggering a refresh)</li>
 * </ul>
 *
 * <p>This exception extends {@link RuntimeException} to avoid forcing
 * upstream methods to declare it.
 */
public class ExpiredJwtException
        extends RuntimeException {

    /**
     * Constructs a new ExpiredJwtException with the specified detail message.
     *
     * @param message the detail message explaining why the exception was thrown
     */
    public ExpiredJwtException(String message) {
        super(message);
    }
}