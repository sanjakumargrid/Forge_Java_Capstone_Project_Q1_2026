package com.talentgrid.gateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Utility component for parsing, validating, and extracting claims from JSON Web Tokens.
 * 
 * <p>This class encapsulates the cryptographic operations necessary to verify the
 * integrity and authenticity of an incoming JWT using a shared HS256 secret. It enforces
 * minimum security standards (e.g., key length) and extracts typed domain claims 
 * (roles, scopes, email) for downstream propagation.</p>
 */
@Component
public class JwtUtil {

    /** Minimum key length in bytes for HS256 (256-bit). */
    private static final int MIN_HS256_KEY_BYTES = 32;

    private final SecretKey signingKey;
    private final String issuer;

    /**
     * Constructs a new {@code JwtUtil}.
     *
     * @param secret the base64-encoded secret key used for HMAC-SHA256 signature verification
     * @param issuer the expected issuer (iss) claim of valid tokens
     */
    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.issuer}") String issuer) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.issuer = issuer;
    }

    @PostConstruct
    void validateSigningKey() {
        int len = signingKey.getEncoded().length;
        if (len < MIN_HS256_KEY_BYTES) {
            throw new IllegalStateException(
                    "jwt.secret must be at least " + MIN_HS256_KEY_BYTES
                            + " bytes (" + (MIN_HS256_KEY_BYTES * 8) + " bits) for HS256; got " + len + " bytes");
        }
    }

    /**
     * Validates the token signature, expiry, and issuer against the configured strict criteria.
     * 
     * @param token the raw JWT string to validate
     * @return the parsed {@link Claims} body if validation succeeds
     * @throws io.jsonwebtoken.JwtException if the token is expired, malformed, or has an invalid signature
     */
    public Claims validateAndParse(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extracts the user's granted roles from the JWT claims.
     * 
     * @param claims the validated JWT claims body
     * @return a normalized list of roles, or an empty list if none exist
     */
    public List<String> extractRoles(Claims claims) {
        return normalizeStringList(claims.get("roles"));
    }

    /**
     * Extracts the subject (user ID) from the JWT claims.
     * 
     * @param claims the validated JWT claims body
     * @return the unique user identifier
     */
    public String extractUserId(Claims claims) {
        return claims.getSubject();
    }

    /**
     * Extracts the unique token identifier (JTI).
     * 
     * @param claims the validated JWT claims body
     * @return the JWT ID
     */
    public String extractJti(Claims claims) {
        return claims.getId();
    }

    /**
     * Extracts the user's granted scopes from the JWT claims.
     * 
     * @param claims the validated JWT claims body
     * @return a normalized list of scopes, or an empty list if none exist
     */
    public List<String> extractScopes(Claims claims) {
        return normalizeStringList(claims.get("scopes"));
    }

    /**
     * Extracts the user's email address from the JWT claims.
     * 
     * @param claims the validated JWT claims body
     * @return the user email, or null if absent
     */
    public String extractEmail(Claims claims) {
        return claims.get("email", String.class);
    }

    private static List<String> normalizeStringList(Object raw) {
        if (raw == null) {
            return Collections.emptyList();
        }
        if (raw instanceof List<?> list) {
            return list.stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }
        if (raw instanceof String s && !s.isBlank()) {
            return Arrays.stream(s.split(","))
                    .map(String::trim)
                    .filter(x -> !x.isEmpty())
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
