package com.talentgrid.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class RequestLoggingFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        log.info("Incoming request: method={}, path={}, ip={}",
                exchange.getRequest().getMethod(),
                exchange.getRequest().getPath(),
                exchange.getRequest().getRemoteAddress());
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -95; // runs right after TraceContext(-99) and CorrelationId(-100), before JWT(-2)
    }
}
