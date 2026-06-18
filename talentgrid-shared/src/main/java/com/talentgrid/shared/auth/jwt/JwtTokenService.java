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

public class JwtTokenService {

    private final String jwtSecret;

    public JwtTokenService(String jwtSecret) {

        this.jwtSecret = jwtSecret;
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
                .userId(Long.valueOf(extractUserId(token)))
                .email(extractEmail(token))
                .roles(extractRoles(token))
                .scopes(extractScopes(token))
                .build();
//        return null;
    }
}