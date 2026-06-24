package com.talentgrid.gateway.constants;

/**
 * Centralized constant definitions for distributed tracing and observability.
 */
public final class TraceConstants {
    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String TRACE_ID_MDC_KEY = "traceId";
    
    private TraceConstants() {
        // Utility class
    }
}
