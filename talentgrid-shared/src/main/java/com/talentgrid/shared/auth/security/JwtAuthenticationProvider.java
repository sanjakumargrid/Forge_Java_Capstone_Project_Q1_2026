package com.talentgrid.shared.auth.security;

import com.talentgrid.shared.auth.jwt.JwtTokenService;
import com.talentgrid.shared.auth.dto.JwtUserContext;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class JwtAuthenticationProvider {

    private final JwtTokenService jwtTokenService;

    public JwtPrincipal authenticate(String token) {

        if (!jwtTokenService.isTokenValid(token)) {
            throw new RuntimeException("Invalid JWT");
        }

        JwtUserContext context =
                jwtTokenService.buildUserContext(token);

        return JwtPrincipal.builder()
                .userId(context.getUserId())
                .email(context.getEmail())
                .roles(context.getRoles())
                .scopes(context.getScopes())
                .build();
    }
}