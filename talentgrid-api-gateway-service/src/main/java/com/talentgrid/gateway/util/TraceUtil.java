package com.talentgrid.gateway.util;

import io.opentelemetry.api.trace.Span;
import org.springframework.stereotype.Component;

@Component
public class TraceUtil {

    public String getCurrentTraceId() {
        Span currentSpan = Span.current();
        if (currentSpan != null && currentSpan.getSpanContext().isValid()) {
            return currentSpan.getSpanContext().getTraceId();
        }
        return null;
    }
}
