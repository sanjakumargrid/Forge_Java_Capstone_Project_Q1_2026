package com.talentgrid.auth.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserStatusRequest {

    @NotNull(message = "Enabled status is required")
    private Boolean enabled;
}
