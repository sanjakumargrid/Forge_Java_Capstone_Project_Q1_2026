package com.talentgrid.gateway.constants;

public final class AppConstants {
    public static final String REDIS_BLACKLIST_PREFIX = "blacklist:";
    public static final long MAX_CONTENT_LENGTH = 10 * 1024 * 1024; // 10MB
    public static final String CORRELATION_ID_MDC_KEY = "correlationId";

    private AppConstants() {
        // Utility class
    }
}
