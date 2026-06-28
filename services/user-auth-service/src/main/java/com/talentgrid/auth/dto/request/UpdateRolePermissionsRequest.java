package com.talentgrid.auth.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
public class UpdateRolePermissionsRequest {

    @NotEmpty(message = "At least one permission is required")
    private Set<String> permissions;
}
