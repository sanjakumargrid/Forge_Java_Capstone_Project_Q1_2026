package com.talentgrid.shared.auth.exception;

/**
 * General exception thrown when a user attempts to access a resource
 * without proper authentication or when their authentication is revoked.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Signal an unauthorized access attempt</li>
 *   <li>Provide a unified exception for general auth failures
 *       (e.g., blacklisted token, mismatched authVersion)</li>
 * </ul>
 */
public class UnauthorizedException
        extends RuntimeException {

    /**
     * Constructs a new UnauthorizedException with the specified detail message.
     *
     * @param message the detail message explaining why the access was denied
     */
    public UnauthorizedException(String message) {
        super(message);
    }
}