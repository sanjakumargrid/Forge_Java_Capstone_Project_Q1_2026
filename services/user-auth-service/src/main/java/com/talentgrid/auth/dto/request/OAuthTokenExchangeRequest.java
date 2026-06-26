package com.talentgrid.auth.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Legacy request body for OAuth token exchange.
 *
 * <p>Exchange codes are now delivered via the HttpOnly {@code FORGE_OAUTH_EXCHANGE} cookie;
 * this DTO is retained for compatibility but is no longer used by {@code POST /oauth/token}.
 */
@Getter
@Setter
@NoArgsConstructor
public class OAuthTokenExchangeRequest {

    private String code;
}
