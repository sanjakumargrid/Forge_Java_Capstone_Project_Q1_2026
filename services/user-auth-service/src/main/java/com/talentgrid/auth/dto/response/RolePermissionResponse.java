package com.talentgrid.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.Set;

@Getter
@Builder
@AllArgsConstructor
public class RolePermissionResponse {

    private final Long roleId;
    private final String roleName;
    private final Set<String> permissions;
}
