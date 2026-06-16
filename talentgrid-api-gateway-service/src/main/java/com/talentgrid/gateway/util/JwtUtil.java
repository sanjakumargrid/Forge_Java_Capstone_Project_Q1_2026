package com.talentgrid.gateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class JwtUtil {

    private final SecretKey signingKey;
    private final String issuer;

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.issuer}") String issuer) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.issuer = issuer;
    }

    /**
     * Validates the token signature, expiry, and issuer.
     * Returns parsed claims on success, throws JwtException on any failure.
     */
    public Claims validateAndParse(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .requireIssuer(issuer)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Extracts the roles claim. JWT should contain: { "roles": ["ADMIN",
     * "HIRING_MANAGER"], ... }
     */
    @SuppressWarnings("unchecked")
    public java.util.List<String> extractRoles(Claims claims) {
        return claims.get("roles", java.util.List.class);
    }

    /**
     * Extracts the subject (userId / employee ID).
     */
    public String extractUserId(Claims claims) {
        return claims.getSubject();
    }

    /**
     * Extracts the Token ID (JTI).
     */
    public String extractJti(Claims claims) {
        return claims.getId();
    }

    /**
     * Extracts the Scopes claim. JWT should contain: { "scopes": ["USER_CREATE", "USER_VIEW"], ... }
     */
    @SuppressWarnings("unchecked")
    public java.util.List<String> extractScopes(Claims claims) {
        return claims.get("scopes", java.util.List.class);
    }

    /**
     * Extracts the Email claim.
     */
    public String extractEmail(Claims claims) {
        return claims.get("email", String.class);
    }
}
