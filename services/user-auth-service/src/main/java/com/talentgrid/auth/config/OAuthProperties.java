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
     * Relative path on the frontend that receives the one-time OAuth exchange code.
     */
    private String callbackPath = "/auth/callback";

    /**
     * Relative path on the frontend used for login error display.
     */
    private String loginErrorPath = "/login";

    /**
     * TTL in seconds for the one-time OAuth exchange code stored in Redis.
     */
    private long exchangeCodeTtlSeconds = 60;
}
