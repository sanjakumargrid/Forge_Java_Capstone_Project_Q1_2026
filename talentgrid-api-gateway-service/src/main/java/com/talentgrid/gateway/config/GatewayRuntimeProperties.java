package com.talentgrid.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Operational toggles for the API gateway (non-route configuration).
 */
@ConfigurationProperties(prefix = "talentgrid.gateway")
public class GatewayRuntimeProperties {

    /**
     * When true, log each incoming request at INFO (useful in lower environments).
     * Default false to avoid log volume and cost in production; enable DEBUG on
     * {@code com.talentgrid.gateway.filter.RequestLoggingFilter} for troubleshooting.
     */
    private boolean requestLoggingEnabled = false;

    public boolean isRequestLoggingEnabled() {
        return requestLoggingEnabled;
    }

    public void setRequestLoggingEnabled(boolean requestLoggingEnabled) {
        this.requestLoggingEnabled = requestLoggingEnabled;
    }
}
