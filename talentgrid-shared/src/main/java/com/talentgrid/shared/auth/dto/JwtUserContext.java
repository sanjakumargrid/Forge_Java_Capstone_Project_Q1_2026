package com.talentgrid.shared.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtUserContext {

    private Long userId;

    private String email;

    private List<String> roles;

    private List<String> scopes;
}