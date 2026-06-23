package com.talentgrid.shared.auth.exception;

/**
 * Exception thrown when a JWT token is malformed, has an invalid signature,
 * or fails validation checks (such as an incorrect token_type).
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Signal that a token is structurally invalid or cryptographically compromised</li>
 *   <li>Differentiate from natural token expiration ({@link ExpiredJwtException})</li>
 * </ul>
 *
 * <p>Consuming services should typically respond with a 401 Unauthorized
 * when this exception is thrown.
 */
public class InvalidJwtException
        extends RuntimeException {

    /**
     * Constructs a new InvalidJwtException with the specified detail message.
     *
     * @param message the detail message explaining why the exception was thrown
     */
    public InvalidJwtException(String message) {
        super(message);
    }
}