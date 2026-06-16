package com.talentgrid.gateway.security;

import com.talentgrid.gateway.constants.HeaderConstants;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class SecurityHeadersFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpResponse response = exchange.getResponse();
        
        response.getHeaders().add(HeaderConstants.X_FRAME_OPTIONS, "DENY");
        response.getHeaders().add(HeaderConstants.X_CONTENT_TYPE_OPTIONS, "nosniff");
        response.getHeaders().add(HeaderConstants.STRICT_TRANSPORT_SECURITY, "max-age=31536000; includeSubDomains; preload");
        response.getHeaders().add(HeaderConstants.CONTENT_SECURITY_POLICY, "default-src 'self'");
        response.getHeaders().add(HeaderConstants.X_XSS_PROTECTION, "1; mode=block");

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -10; // High priority, runs early
    }
}
