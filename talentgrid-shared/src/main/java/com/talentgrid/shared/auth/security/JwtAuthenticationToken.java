package com.talentgrid.shared.auth.security;

import lombok.Getter;

/**
 * Lightweight wrapper that holds a {@link JwtPrincipal} after
 * successful JWT authentication.
 *
 * <p>Used to pass the authenticated principal between the
 * JWT filter and Spring Security context population in
 * consuming microservices.
 *
 * @see JwtPrincipal
 * @see JwtAuthenticationProvider
 */
@Getter
public class JwtAuthenticationToken {

    /** The authenticated user principal extracted from the JWT. */
    private final JwtPrincipal principal;

    /**
     * Creates a new authentication token wrapping the given principal.
     *
     * @param principal the authenticated user principal
     */
    public JwtAuthenticationToken(
            JwtPrincipal principal
    ) {
        this.principal = principal;
    }
}