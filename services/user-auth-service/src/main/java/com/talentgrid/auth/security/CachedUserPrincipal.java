package com.talentgrid.auth.security;

import lombok.Builder;
import lombok.Getter;

/**
 * Immutable principal representing an authenticated user within the auth-service.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Hold the user's identity and metadata extracted from the Redis cache</li>
 *   <li>Represent the {@code Authentication.principal} in the Spring Security context</li>
 *   <li>Provide fast access to user status (enabled, authVersion) without DB queries</li>
 * </ul>
 */
@Getter
@Builder
public class CachedUserPrincipal {

    /** The unique database identifier of the user. */
    private final Long userId;

    /** The email address of the user. */
    private final String email;

    /** Whether the user account is currently enabled. */
    private final Boolean enabled;

    /** The current authorization version (used for invalidation). */
    private final Long authVersion;

    /**
     * Override toString to provide a meaningful representation
     * (often used implicitly by some security evaluators).
     */
    @Override
    public String toString() {
        return email;
    }
}