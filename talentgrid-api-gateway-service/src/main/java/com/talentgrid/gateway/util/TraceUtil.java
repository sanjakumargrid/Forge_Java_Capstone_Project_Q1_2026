package com.talentgrid.gateway.util;

import io.opentelemetry.api.trace.Span;
import org.springframework.stereotype.Component;

/**
 * Utility for interacting with the current distributed tracing context.
 * 
 * <p>Leverages OpenTelemetry/Micrometer abstractions to safely extract the current 
 * trace ID without coupling the Gateway directly to a specific APM vendor implementation.</p>
 */
@Component
public class TraceUtil {

    /**
     * Retrieves the active trace ID from the current tracing context.
     * 
     * @return the trace ID string, or {@code null} if no active trace context exists
     */
    public String getCurrentTraceId() {
        Span currentSpan = Span.current();
        if (currentSpan != null && currentSpan.getSpanContext().isValid()) {
            return currentSpan.getSpanContext().getTraceId();
        }
        return null;
    }
}
