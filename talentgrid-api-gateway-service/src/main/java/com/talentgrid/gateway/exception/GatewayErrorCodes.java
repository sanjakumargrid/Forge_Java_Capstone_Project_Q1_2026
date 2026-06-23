package com.talentgrid.gateway.exception;

/**
 * Stable, machine-readable error codes included in JSON error bodies.
 * 
 * <p>These codes are utilized by downstream clients (e.g., front-end applications) 
 * and support tooling to definitively identify the root cause of an error occurring 
 * at the Gateway layer, decoupling error identification from localized HTTP messages.</p>
 */
public final class GatewayErrorCodes {

    public static final String UNAUTHORIZED = "GATEWAY_UNAUTHORIZED";
    public static final String FORBIDDEN = "GATEWAY_FORBIDDEN";
    public static final String NO_RBAC_RULE = "GATEWAY_NO_RBAC_RULE";
    public static final String PAYLOAD_TOO_LARGE = "GATEWAY_PAYLOAD_TOO_LARGE";
    public static final String DOWNSTREAM_UNAVAILABLE = "GATEWAY_DOWNSTREAM_UNAVAILABLE";
    public static final String DOWNSTREAM_BAD_GATEWAY = "GATEWAY_DOWNSTREAM_BAD_GATEWAY";
    public static final String DOWNSTREAM_TIMEOUT = "GATEWAY_DOWNSTREAM_TIMEOUT";
    public static final String INTERNAL = "GATEWAY_INTERNAL";

    private GatewayErrorCodes() {
        // Utility class
    }
}
