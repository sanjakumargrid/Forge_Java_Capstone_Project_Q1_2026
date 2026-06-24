package com.talentgrid.shared.auth.security;

import lombok.Getter;

@Getter
public class JwtAuthenticationToken {

    private final JwtPrincipal principal;

    public JwtAuthenticationToken(
            JwtPrincipal principal
    ) {
        this.principal = principal;
    }
}