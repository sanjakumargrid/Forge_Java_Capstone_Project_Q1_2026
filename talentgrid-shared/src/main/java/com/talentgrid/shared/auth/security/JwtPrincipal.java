package com.talentgrid.shared.auth.security;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JwtPrincipal {

    private Long userId;

    private String email;

    private List<String> roles;

    private List<String> scopes;
}