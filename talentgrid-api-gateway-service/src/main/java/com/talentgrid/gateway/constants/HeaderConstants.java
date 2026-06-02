package com.talentgrid.gateway.constants;

public final class HeaderConstants {
    public static final String USER_ID = "X-User-Id";
    public static final String USER_ROLES = "X-User-Roles";
    public static final String USER_SCOPES = "X-User-Scopes";
    public static final String USER_EMAIL = "X-User-Email";
    public static final String AUTH_TIME = "X-Auth-Time";
    public static final String CORRELATION_ID = "X-Correlation-Id";

    // Security Headers
    public static final String X_FRAME_OPTIONS = "X-Frame-Options";
    public static final String X_CONTENT_TYPE_OPTIONS = "X-Content-Type-Options";
    public static final String STRICT_TRANSPORT_SECURITY = "Strict-Transport-Security";
    public static final String CONTENT_SECURITY_POLICY = "Content-Security-Policy";
    public static final String X_XSS_PROTECTION = "X-XSS-Protection";

    private HeaderConstants() {
        // Utility class
    }
}
