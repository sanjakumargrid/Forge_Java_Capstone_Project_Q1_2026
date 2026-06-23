package com.talentgrid.shared.auth.security;

import lombok.*;

import java.util.List;

/**
 * Immutable principal object representing an authenticated user
 * extracted from a validated JWT token.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Carry authenticated user identity through Spring Security context</li>
 *   <li>Provide user metadata (roles, scopes, authVersion) to downstream handlers</li>
 * </ul>
 *
 * <p>This principal is set as the {@code Authentication.principal} in
 * Spring Security's {@code SecurityContextHolder} by consuming microservices
 * after JWT validation via {@link JwtAuthenticationProvider}.
 *
 * @see JwtAuthenticationProvider
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JwtPrincipal {

    /** The unique database identifier of the authenticated user. */
    private Long userId;

    /** The email address of the authenticated user. */
    private String email;

    /** The role names assigned to the user (e.g., ADMIN, EMPLOYEE). */
    private List<String> roles;

    /** The scope (permission) names granted to the user (e.g., DEMAND_CREATE). */
    private List<String> scopes;

    /** The authorization version at the time the JWT was issued. */
    private Long authVersion;
}