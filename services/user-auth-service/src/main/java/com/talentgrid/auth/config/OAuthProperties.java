package com.talentgrid.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration for the Google OAuth2 browser login flow.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.oauth")
public class OAuthProperties {

    /**
     * Frontend base URL used for success and error redirects (no trailing slash).
     */
    private String frontendBaseUrl = "http://localhost:4200";

    /**
     * Allowed email domain suffix (case-insensitive), e.g. {@code @griddynamics.com}.
     */
    private String allowedEmailDomain = "@griddynamics.com";

    /**
     * Relative path on the frontend OAuth callback page (no query parameters).
     */
    private String callbackPath = "/auth/callback";

    /**
     * Relative path on the frontend used for login error display.
     */
    private String loginErrorPath = "/login";

    /**
     * HttpOnly cookie name carrying the one-time OAuth exchange code.
     */
    private String exchangeCodeCookieName = "FORGE_OAUTH_EXCHANGE";

    /**
     * Cookie path scoped to auth API endpoints.
     */
    private String exchangeCodeCookiePath = "/api/v1/auth";

    /**
     * SameSite policy for the exchange-code cookie.
     */
    private String exchangeCodeCookieSameSite = "Lax";

    /**
     * Whether the exchange-code cookie requires HTTPS ({@code Secure} flag).
     */
    private boolean exchangeCodeCookieSecure = false;  // override to true in production via OAUTH_EXCHANGE_COOKIE_SECURE=true

    /**
     * TTL in seconds for the one-time OAuth exchange code stored in Redis.
     */
    private long exchangeCodeTtlSeconds = 300;
}
