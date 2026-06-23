package com.talentgrid.gateway.filter;

import com.talentgrid.gateway.config.GatewayRuntimeProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Global filter responsible for logging incoming HTTP requests.
 * 
 * <p>Logs the HTTP method, path, and remote IP address. The logging level (INFO vs DEBUG) 
 * is determined by the {@link GatewayRuntimeProperties#isRequestLoggingEnabled()} configuration.</p>
 * 
 * <p>Implements {@link GlobalFilter} and {@link Ordered} to execute early in the filter chain,
 * after correlation and trace IDs are generated, but before security and payload validation.</p>
 */
@Component
public class RequestLoggingFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    private final GatewayRuntimeProperties gatewayRuntimeProperties;

    /**
     * Constructs a new {@code RequestLoggingFilter}.
     *
     * @param gatewayRuntimeProperties the runtime properties for the gateway
     */
    public RequestLoggingFilter(GatewayRuntimeProperties gatewayRuntimeProperties) {
        this.gatewayRuntimeProperties = gatewayRuntimeProperties;
    }

    /**
     * Logs the incoming request details.
     *
     * @param exchange the current server web exchange
     * @param chain    the gateway filter chain
     * @return a {@link Mono} that indicates when request processing is complete
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (gatewayRuntimeProperties.isRequestLoggingEnabled()) {
            log.info("Incoming request: method={}, path={}, ip={}",
                    exchange.getRequest().getMethod(),
                    exchange.getRequest().getPath(),
                    exchange.getRequest().getRemoteAddress());
        } else if (log.isDebugEnabled()) {
            log.debug("Incoming request: method={}, path={}, ip={}",
                    exchange.getRequest().getMethod(),
                    exchange.getRequest().getPath(),
                    exchange.getRequest().getRemoteAddress());
        }
        return chain.filter(exchange);
    }

    /**
     * Determines the execution order of this filter.
     * 
     * @return the order value (-95), ensuring it runs right after TraceContext (-99) and 
     *         CorrelationId (-100), but before JwtAuthenticationFilter (-2).
     */
    @Override
    public int getOrder() {
        return -95;
    }
}
