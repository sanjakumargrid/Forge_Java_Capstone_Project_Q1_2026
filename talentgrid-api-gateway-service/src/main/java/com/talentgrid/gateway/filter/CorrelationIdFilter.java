package com.talentgrid.gateway.filter;

import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import com.talentgrid.gateway.constants.AppConstants;
import com.talentgrid.gateway.constants.HeaderConstants;

import java.util.UUID;

/**
 * Global filter responsible for ensuring every incoming request has a unique correlation ID.
 * 
 * <p>This filter extracts the correlation ID from the request headers. If one does not exist,
 * a new UUID is generated. The correlation ID is then injected into the SLF4J MDC context
 * for structured logging, added to the downstream request headers, and appended to the
 * HTTP response headers for client-side tracking and debugging.</p>
 * 
 * <p>Implements {@link GlobalFilter} and {@link Ordered} to execute early in the filter chain.</p>
 */
@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {

    /**
     * intercepts the request to inject or propagate a correlation ID.
     *
     * @param exchange the current server web exchange
     * @param chain    the gateway filter chain
     * @return a {@link Mono} that indicates when request processing is complete
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String correlationId = request.getHeaders().getFirst(HeaderConstants.CORRELATION_ID);

        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = UUID.randomUUID().toString();
            request = exchange.getRequest().mutate()
                    .header(HeaderConstants.CORRELATION_ID, correlationId)
                    .build();
        }

        MDC.put(AppConstants.CORRELATION_ID_MDC_KEY, correlationId);
        exchange.getAttributes().put(HeaderConstants.CORRELATION_ID, correlationId);

        final String finalCorrelationId = correlationId;
        ServerWebExchange mutatedExchange = exchange.mutate().request(request).build();
        
        mutatedExchange.getResponse().beforeCommit(() -> {
            mutatedExchange.getResponse().getHeaders()
                    .addIfAbsent(HeaderConstants.CORRELATION_ID, finalCorrelationId);
            return Mono.empty();
        });

        return chain.filter(mutatedExchange)
                .doFinally(signalType -> MDC.remove(AppConstants.CORRELATION_ID_MDC_KEY));
    }

    /**
     * Determines the execution order of this filter.
     * 
     * @return the order value (-100) ensuring it runs very early in the chain.
     */
    @Override
    public int getOrder() {
        return -100;
    }
}
