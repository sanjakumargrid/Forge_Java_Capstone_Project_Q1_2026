package com.talentgrid.gateway.filter;

import com.talentgrid.gateway.constants.TraceConstants;
import com.talentgrid.gateway.util.TraceUtil;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Global filter responsible for extracting and propagating distributed tracing contexts.
 * 
 * <p>This filter utilizes {@link TraceUtil} to retrieve the current OpenTelemetry/Micrometer
 * trace ID. If present, it injects the trace ID into the SLF4J MDC context for correlated
 * logging and adds it as a header (defined in {@link TraceConstants#TRACE_ID_HEADER}) 
 * for downstream microservices to consume.</p>
 * 
 * <p>Implements {@link GlobalFilter} and {@link Ordered} to execute early in the filter chain,
 * immediately after the {@code CorrelationIdFilter}.</p>
 */
@Component
public class TraceContextFilter implements GlobalFilter, Ordered {

    private final TraceUtil traceUtil;

    /**
     * Constructs a new {@code TraceContextFilter}.
     *
     * @param traceUtil utility for extracting current trace information
     */
    public TraceContextFilter(TraceUtil traceUtil) {
        this.traceUtil = traceUtil;
    }

    /**
     * Extracts the trace ID and propagates it to MDC and downstream headers.
     *
     * @param exchange the current server web exchange
     * @param chain    the gateway filter chain
     * @return a {@link Mono} that indicates when request processing is complete
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String traceId = traceUtil.getCurrentTraceId();
        
        if (traceId != null) {
            MDC.put(TraceConstants.TRACE_ID_MDC_KEY, traceId);
            ServerWebExchange mutatedExchange = exchange.mutate()
                .request(r -> r.header(TraceConstants.TRACE_ID_HEADER, traceId))
                .build();
            return chain.filter(mutatedExchange)
                    .doFinally(signalType -> MDC.remove(TraceConstants.TRACE_ID_MDC_KEY));
        }
        
        return chain.filter(exchange);
    }

    /**
     * Determines the execution order of this filter.
     * 
     * @return the order value (-99), ensuring it runs right after the CorrelationIdFilter (-100)
     */
    @Override
    public int getOrder() {
        return -99;
    }
}
