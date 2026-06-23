package com.talentgrid.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Global filter responsible for logging outgoing HTTP responses and calculating request latency.
 * 
 * <p>This filter records the start time of a request during the "pre" phase and logs the 
 * final HTTP status code, path, and total latency (in milliseconds) during the "post" phase.</p>
 * 
 * <p>Implements {@link GlobalFilter} and {@link Ordered} to execute with highest precedence 
 * (which ensures its "post" execution phase runs last, capturing the true end-to-end latency).</p>
 */
@Component
public class ResponseLoggingFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(ResponseLoggingFilter.class);
    private static final String START_TIME_ATTR = "gatewayRequestStartTime";

    /**
     * Intercepts the request to mark the start time, and logs the response details upon completion.
     *
     * @param exchange the current server web exchange
     * @param chain    the gateway filter chain
     * @return a {@link Mono} that indicates when request processing is complete
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        exchange.getAttributes().put(START_TIME_ATTR, System.currentTimeMillis());

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            Long startTime = exchange.getAttribute(START_TIME_ATTR);
            long latencyMs = (startTime != null) ? System.currentTimeMillis() - startTime : -1;

            log.info("Outgoing response: status={}, path={}, latency={}ms",
                    exchange.getResponse().getStatusCode(),
                    exchange.getRequest().getPath(),
                    latencyMs);
        }));
    }

    /**
     * Determines the execution order of this filter.
     * 
     * @return {@link Ordered#HIGHEST_PRECEDENCE} ensuring the post-filter phase runs last
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
