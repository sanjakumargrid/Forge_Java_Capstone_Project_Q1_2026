package com.talentgrid.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.Set;

@Getter
@Builder
@AllArgsConstructor
public class LoginResponse {

    private final String accessToken;

    private final String type;

    private final String email;

    private final Set<String> roles;
}