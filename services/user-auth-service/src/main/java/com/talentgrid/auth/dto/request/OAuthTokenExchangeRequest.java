package com.talentgrid.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request body for exchanging a one-time Google OAuth code for JWT tokens.
 */
@Getter
@Setter
@NoArgsConstructor
public class OAuthTokenExchangeRequest {

    @NotBlank(message = "OAuth exchange code is required")
    private String code;
}
