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

@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {

    // Replaced by HeaderConstants.CORRELATION_ID

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

        // Inject the correlation ID into the response headers for frontend debugging
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

    @Override
    public int getOrder() {
        return -100;
    }
}
