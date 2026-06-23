package com.talentgrid.auth.jwt;

import com.talentgrid.auth.entity.User;
import com.talentgrid.shared.auth.constants.JwtConstants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import java.util.function.Function;

/**
 * Service responsible for generating and parsing JWT access tokens and
 * managing refresh tokens within the auth-service.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Generate cryptographically signed JWT access tokens</li>
 *   <li>Embed user identity, roles, scopes, and authVersion claims</li>
 *   <li>Generate opaque refresh tokens</li>
 *   <li>Parse and validate incoming tokens during authentication</li>
 * </ul>
 *
 * <p>This service caches the {@link SecretKey} and {@link JwtParser}
 * after initialization to avoid the performance overhead of rebuilding
 * them on every request.
 */
@Service
public class JwtService {

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Value("${jwt.issuer}")
    private String issuer;

    private SecretKey signingKey;

    private JwtParser jwtParser;

    /**
     * Initializes the cryptographic key and parser after Spring
     * injects the property values.
     */
    @PostConstruct
    protected void init() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);

        this.jwtParser = Jwts.parser()
                .verifyWith(this.signingKey)
                .build();
    }

    /**
     * Generates a new JWT access token for the authenticated user.
     *
     * <p>The token includes standard claims (sub, iss, aud, jti, exp, iat)
     * and custom claims (email, authVersion, token_type, roles, scopes).
     *
     * @param user the authenticated user entity
     * @return the signed JWT string
     */
    public String generateToken(User user) {

        return Jwts.builder()
                .subject(String.valueOf(user.getId()))

                .claim(JwtConstants.EMAIL, user.getEmail())

                .claim(
                        JwtConstants.AUTH_VERSION,
                        user.getAuthVersion()
                )

                .issuer(issuer)

                .id(UUID.randomUUID().toString())

                .claim(JwtConstants.TOKEN_TYPE, JwtConstants.ACCESS_TOKEN)

                .audience()
                .add(JwtConstants.API_GATEWAY)
                .and()

                // ADD ROLES
                .claim(
                        JwtConstants.ROLES,
                        user.getRoles()
                                .stream()
                                .map(role -> role.getName())
                                .toList()
                )

                // ADD SCOPES
                .claim(
                        JwtConstants.SCOPES,
                        user.getRoles()
                                .stream()
                                .flatMap(role ->
                                        role.getScopes().stream()
                                )
                                .map(scope -> scope.getName())
                                .distinct()
                                .toList()
                )

                .issuedAt(new Date())

                .expiration(
                        new Date(
                                System.currentTimeMillis()
                                        + jwtExpiration
                        )
                )

                .signWith(signingKey)
                .compact();
    }

    /**
     * Generates a cryptographically random opaque string to be used
     * as a refresh token.
     *
     * @return a random UUID string
     */
    public String generateRefreshToken() {

        return UUID.randomUUID().toString();
    }

    /**
     * Returns the configured refresh token expiration duration.
     *
     * @return duration in milliseconds
     */
    public long getRefreshExpiration() {

        return refreshExpiration;
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

        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extracts the expiration date from the JWT.
     *
     * @param token the signed JWT string
     * @return the token's expiration timestamp
     */
    public Date extractExpiration(String token) {

        return extractClaim(token, Claims::getExpiration);
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
     * Validates that the token belongs to the given user and is not expired.
     *
     * @param token       the signed JWT string
     * @param userDetails the user details to validate against
     * @return {@code true} if valid and belongs to user
     */
    public boolean isTokenValid(
            String token,
            UserDetails userDetails
    ) {

        final String email = extractEmail(token);

        return email.equals(userDetails.getUsername())
                && !isTokenExpired(token);
    }

    /**
     * Checks whether the JWT has expired.
     *
     * @param token the signed JWT string
     * @return {@code true} if the token's expiration is before the current time
     */
    private boolean isTokenExpired(String token) {

        return extractExpiration(token)
                .before(new Date());
    }

    /**
     * Extracts a specific claim from the JWT using a resolver function.
     *
     * @param token          the signed JWT string
     * @param claimsResolver function to extract the desired claim from {@link Claims}
     * @param <T>            the claim value type
     * @return the extracted claim value
     */
    private <T> T extractClaim(
            String token,
            Function<Claims, T> claimsResolver
    ) {

        final Claims claims = extractAllClaims(token);

        return claimsResolver.apply(claims);
    }

    /**
     * Parses the JWT and returns all claims after signature verification.
     *
     * @param token the signed JWT string
     * @return the complete set of JWT claims
     * @throws io.jsonwebtoken.JwtException if the token is invalid or verification fails
     */
    private Claims extractAllClaims(String token) {

        return jwtParser
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extracts the unique token identifier (JTI) from the JWT.
     *
     * @param token the signed JWT string
     * @return the JTI claim value
     */
    public String extractJti(String token) {

        return extractAllClaims(token).getId();
    }

    /**
     * Extracts the authorization version from the JWT claims.
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

}