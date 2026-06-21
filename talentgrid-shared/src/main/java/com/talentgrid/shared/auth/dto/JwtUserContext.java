package com.talentgrid.shared.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Data transfer object representing the user context extracted
 * from JWT claims during token parsing.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Carry parsed JWT claim data between service layers</li>
 *   <li>Provide a structured representation of JWT user identity</li>
 * </ul>
 *
 * <p>This DTO is populated by {@link com.talentgrid.shared.auth.jwt.JwtTokenService#buildUserContext(String)}
 * and consumed by {@link com.talentgrid.shared.auth.security.JwtAuthenticationProvider}
 * to build the authenticated principal.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtUserContext {

    /** The unique database identifier of the user (from JWT subject). */
    private Long userId;

    /** The email address of the user (from JWT custom claim). */
    private String email;

    /** The role names assigned to the user. */
    private List<String> roles;

    /** The scope (permission) names granted to the user. */
    private List<String> scopes;

    /** The authorization version at the time the JWT was issued. */
    private Long authVersion;
}