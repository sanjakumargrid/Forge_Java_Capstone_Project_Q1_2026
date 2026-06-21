package com.talentgrid.shared.auth.security;

import com.talentgrid.shared.auth.constants.JwtConstants;
import com.talentgrid.shared.auth.dto.JwtUserContext;
import com.talentgrid.shared.auth.exception.ExpiredJwtException;
import com.talentgrid.shared.auth.exception.InvalidJwtException;
import com.talentgrid.shared.auth.jwt.JwtTokenService;
import lombok.RequiredArgsConstructor;

/**
 * Authenticates incoming requests by validating JWT tokens
 * and producing a {@link JwtPrincipal} for Spring Security context population.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Validate JWT signature and expiration</li>
 *   <li>Verify token type is "access"</li>
 *   <li>Extract user context from validated claims</li>
 *   <li>Build an authenticated {@link JwtPrincipal}</li>
 * </ul>
 *
 * <p>This provider is used by downstream microservices (e.g., demand-service)
 * that consume the shared-auth module. It does NOT perform blacklist
 * or authVersion checks — those are auth-service responsibilities.
 *
 * @see JwtTokenService
 * @see JwtPrincipal
 */
@RequiredArgsConstructor
public class JwtAuthenticationProvider {

    private final JwtTokenService jwtTokenService;

    /**
     * Validates the given JWT token and creates an authenticated
     * {@link JwtPrincipal} containing the user's identity and authorities.
     *
     * <p>Validation steps:
     * <ol>
     *   <li>Parse and verify JWT signature</li>
     *   <li>Check token expiration</li>
     *   <li>Verify token_type is "access"</li>
     *   <li>Extract user context (userId, email, roles, scopes, authVersion)</li>
     * </ol>
     *
     * @param token the raw JWT string (without "Bearer " prefix)
     * @return an authenticated {@link JwtPrincipal} with user details
     * @throws ExpiredJwtException if the token has expired
     * @throws InvalidJwtException if the token is malformed, has an invalid
     *                             signature, or is not an access token
     */
    public JwtPrincipal authenticate(String token) {

        try {

            if (!jwtTokenService.isTokenValid(token)) {
                throw new InvalidJwtException(
                        "JWT token has expired"
                );
            }

            // Reject non-access tokens (e.g., refresh tokens)
            String tokenType =
                    jwtTokenService.extractTokenType(token);

            if (!JwtConstants.ACCESS_TOKEN.equals(tokenType)) {
                throw new InvalidJwtException(
                        "Invalid token type: expected access token"
                );
            }

            JwtUserContext context =
                    jwtTokenService.buildUserContext(token);

            return JwtPrincipal.builder()
                    .userId(context.getUserId())
                    .email(context.getEmail())
                    .roles(context.getRoles())
                    .scopes(context.getScopes())
                    .authVersion(context.getAuthVersion())
                    .build();

        } catch (io.jsonwebtoken.ExpiredJwtException e) {

            throw new ExpiredJwtException(
                    "JWT token has expired: " + e.getMessage()
            );

        } catch (ExpiredJwtException | InvalidJwtException e) {

            // Re-throw our own exceptions as-is
            throw e;

        } catch (Exception e) {

            throw new InvalidJwtException(
                    "JWT validation failed: " + e.getMessage()
            );
        }
    }
}