package com.talentgrid.shared.auth.jwt;

import com.talentgrid.shared.auth.constants.JwtConstants;
import com.talentgrid.shared.auth.dto.JwtUserContext;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

public class JwtTokenService {

    private final String jwtSecret;

    public JwtTokenService(String jwtSecret) {

        this.jwtSecret = jwtSecret;
    }

    /**
     * Generates a short-lived internal token for service-to-service communication
     * where no user context exists (e.g., webhooks, Kafka consumers).
     */
    public String generateInternalServiceToken() {
        return Jwts.builder()
                .subject("0")
                .claim(JwtConstants.EMAIL, JwtConstants.INTERNAL_SERVICE_EMAIL)
                .claim(JwtConstants.TOKEN_TYPE, JwtConstants.ACCESS_TOKEN)
                .claim(JwtConstants.ROLES, List.of("ROLE_SYSTEM"))
                .claim(JwtConstants.SCOPES, List.of(
                        "CANDIDATE_VIEW", "CANDIDATE_CREATE", "CANDIDATE_UPDATE",
                        "APPLICATION_VIEW", "APPLICATION_CREATE", "APPLICATION_UPDATE",
                        "OFFER_VIEW", "OFFER_CREATE", "OFFER_UPDATE",
                        "INTERVIEW_VIEW", "INTERVIEW_CREATE", "INTERVIEW_UPDATE",
                        "DEMAND_VIEW", "DEMAND_CREATE", "DEMAND_UPDATE"
                ))
                .id(java.util.UUID.randomUUID().toString())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 5)) // 5 minutes
                .signWith(getSigningKey())
                .compact();
    }

    public String extractEmail(String token) {

        return extractAllClaims(token)
                .get(JwtConstants.EMAIL, String.class);
    }

    public String extractUserId(String token) {

        return extractClaim(
                token,
                Claims::getSubject
        );
    }

    public String extractTokenType(String token) {

        return extractAllClaims(token)
                .get(JwtConstants.TOKEN_TYPE, String.class);
    }

    public String extractJti(String token) {

        return extractAllClaims(token)
                .getId();
    }

    public Date extractExpiration(String token) {

        return extractClaim(
                token,
                Claims::getExpiration
        );
    }

    public boolean isTokenExpired(String token) {

        return extractExpiration(token)
                .before(new Date());
    }

    public boolean isTokenValid(String token) {

        return !isTokenExpired(token);
    }

    private <T> T extractClaim(
            String token,
            Function<Claims, T> resolver
    ) {

        return resolver.apply(
                extractAllClaims(token)
        );
    }

    public Claims extractAllClaims(String token) {

        return Jwts.parser()
                .verifyWith(getSigningKey())
//                .requireAudience("api-gateway")   // ADD THIS
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {

        byte[] keyBytes =
                jwtSecret.getBytes(StandardCharsets.UTF_8);

        return Keys.hmacShaKeyFor(keyBytes);
    }

    public List<String> extractRoles(String token) {

        return extractAllClaims(token)
                .get(JwtConstants.ROLES, List.class);
    }

    public List<String> extractScopes(String token) {

        return extractAllClaims(token)
                .get(JwtConstants.SCOPES, List.class);
    }

    public JwtUserContext buildUserContext(String token) {

        return JwtUserContext.builder()
                .userId(parseUserId(extractUserId(token)))
                .email(extractEmail(token))
                .roles(extractRoles(token))
                .scopes(extractScopes(token))
                .build();
    }

    /**
     * Parses the JWT subject into a numeric user id. Internal service tokens use a
     * non-numeric subject ("system"), so fall back to a sentinel id instead of
     * throwing NumberFormatException, which would reject every tokenless S2S call.
     */
    private Long parseUserId(String subject) {
        if (subject == null) {
            return null;
        }
        try {
            return Long.valueOf(subject);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}