package com.talentgrid.shared.auth.jwt;

import com.talentgrid.shared.auth.constants.JwtConstants;
import com.talentgrid.shared.auth.dto.JwtUserContext;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

/**
 * Shared JWT token parsing and validation service used by all
 * TalentGrid microservices for stateless authentication.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Parse and validate signed JWT tokens</li>
 *   <li>Extract individual claims (userId, email, roles, scopes, authVersion)</li>
 *   <li>Build {@link JwtUserContext} from token claims</li>
 *   <li>Check token expiration</li>
 * </ul>
 *
 * <p>This service caches the {@link SecretKey} and {@link JwtParser}
 * instances for performance. Both are immutable and thread-safe,
 * making this service safe for concurrent use.
 *
 * <p>This class is instantiated by {@link com.talentgrid.shared.config.JwtAutoConfiguration}
 * and shared across all consuming microservices.
 */
public class JwtTokenService {

    private final SecretKey signingKey;

    private final JwtParser jwtParser;

    /**
     * Creates a new JwtTokenService with the provided secret.
     *
     * <p>The signing key and parser are computed once during construction
     * and reused for all subsequent operations to avoid repeated
     * cryptographic key derivation.
     *
     * @param jwtSecret the HMAC-SHA secret used for JWT signature verification
     */
    public JwtTokenService(String jwtSecret) {

        this.signingKey = Keys.hmacShaKeyFor(
                jwtSecret.getBytes(StandardCharsets.UTF_8)
        );

        this.jwtParser = Jwts.parser()
                .verifyWith(signingKey)
                .build();
    }

    /**
     * Extracts the user's email from the JWT custom claims.
     *
     * @param token the signed JWT string
     * @return the email address stored in the token
     */
    public String extractEmail(String token) {

        return extractAllClaims(token)
                .get(JwtConstants.EMAIL, String.class);
    }

    /**
     * Extracts the user ID from the JWT subject claim.
     *
     * @param token the signed JWT string
     * @return the user ID as a string
     */
    public String extractUserId(String token) {

        return extractClaim(
                token,
                Claims::getSubject
        );
    }

    /**
     * Extracts the token type discriminator from the JWT claims.
     *
     * @param token the signed JWT string
     * @return the token type (e.g., "access" or "refresh")
     */
    public String extractTokenType(String token) {

        return extractAllClaims(token)
                .get(JwtConstants.TOKEN_TYPE, String.class);
    }

    /**
     * Extracts the unique token identifier (JTI) from the JWT.
     *
     * @param token the signed JWT string
     * @return the JTI claim value
     */
    public String extractJti(String token) {

        return extractAllClaims(token)
                .getId();
    }

    /**
     * Extracts the authorization version from the JWT claims.
     *
     * <p>The authVersion is used to invalidate tokens when a user's
     * roles, permissions, or enabled status changes. Returns 1L as
     * default for tokens without this claim (backward compatibility).
     *
     * @param token the signed JWT string
     * @return the authVersion value, or 1L if not present
     */
    public Long extractAuthVersion(String token) {

        Number value =
                extractAllClaims(token)
                        .get(JwtConstants.AUTH_VERSION, Number.class);

        return value == null
                ? 1L
                : value.longValue();
    }

    /**
     * Extracts the expiration date from the JWT.
     *
     * @param token the signed JWT string
     * @return the token's expiration timestamp
     */
    public Date extractExpiration(String token) {

        return extractClaim(
                token,
                Claims::getExpiration
        );
    }

    /**
     * Checks whether the JWT has expired.
     *
     * @param token the signed JWT string
     * @return {@code true} if the token's expiration is before the current time
     */
    public boolean isTokenExpired(String token) {

        return extractExpiration(token)
                .before(new Date());
    }

    /**
     * Validates that the JWT is not expired.
     *
     * @param token the signed JWT string
     * @return {@code true} if the token is still valid (not expired)
     */
    public boolean isTokenValid(String token) {

        return !isTokenExpired(token);
    }

    /**
     * Extracts a specific claim from the JWT using a resolver function.
     *
     * @param token    the signed JWT string
     * @param resolver function to extract the desired claim from {@link Claims}
     * @param <T>      the claim value type
     * @return the extracted claim value
     */
    private <T> T extractClaim(
            String token,
            Function<Claims, T> resolver
    ) {

        return resolver.apply(
                extractAllClaims(token)
        );
    }

    /**
     * Parses the JWT and returns all claims after signature verification.
     *
     * <p>Uses the cached {@link JwtParser} instance for performance.
     *
     * @param token the signed JWT string
     * @return the complete set of JWT claims
     * @throws io.jsonwebtoken.JwtException if the token is invalid or signature verification fails
     */
    public Claims extractAllClaims(String token) {

        return jwtParser
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extracts the list of role names from the JWT claims.
     *
     * @param token the signed JWT string
     * @return list of role name strings
     */
    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {

        return extractAllClaims(token)
                .get(JwtConstants.ROLES, List.class);
    }

    /**
     * Extracts the list of scope (permission) names from the JWT claims.
     *
     * @param token the signed JWT string
     * @return list of scope name strings
     */
    @SuppressWarnings("unchecked")
    public List<String> extractScopes(String token) {

        return extractAllClaims(token)
                .get(JwtConstants.SCOPES, List.class);
    }

    /**
     * Builds a complete {@link JwtUserContext} by parsing the JWT once
     * and extracting all required claims from the parsed result.
     *
     * <p>This is the preferred method for extracting multiple claims,
     * as it avoids redundant JWT parsing and signature verification.
     *
     * @param token the signed JWT string
     * @return a populated {@link JwtUserContext} containing all user claims
     */
    @SuppressWarnings("unchecked")
    public JwtUserContext buildUserContext(String token) {

        // Parse JWT once and extract all claims in a single pass
        Claims claims = extractAllClaims(token);

        Number authVersionValue =
                claims.get(JwtConstants.AUTH_VERSION, Number.class);

        return JwtUserContext.builder()
                .userId(Long.valueOf(claims.getSubject()))
                .email(claims.get(JwtConstants.EMAIL, String.class))
                .roles(claims.get(JwtConstants.ROLES, List.class))
                .scopes(claims.get(JwtConstants.SCOPES, List.class))
                .authVersion(
                        authVersionValue == null
                                ? 1L
                                : authVersionValue.longValue()
                )
                .build();
    }
}