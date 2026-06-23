package com.talentgrid.gateway.security;

import com.talentgrid.gateway.constants.HeaderConstants;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Global filter responsible for injecting standard security headers into all responses.
 * 
 * <p>This filter protects clients against common web vulnerabilities such as Clickjacking,
 * Cross-Site Scripting (XSS), and MIME-sniffing by enforcing strict browser security policies.
 * Headers are only added if they are not already present in the response.</p>
 */
@Component
public class SecurityHeadersFilter implements GlobalFilter, Ordered {

    /**
     * Injects OWASP-recommended security headers into the response.
     *
     * @param exchange the current server web exchange
     * @param chain    the gateway filter chain
     * @return a {@link Mono} that indicates when request processing is complete
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpResponse response = exchange.getResponse();
        HttpHeaders headers = response.getHeaders();

        setHeaderIfAbsent(headers, HeaderConstants.X_FRAME_OPTIONS, "DENY");
        setHeaderIfAbsent(headers, HeaderConstants.X_CONTENT_TYPE_OPTIONS, "nosniff");
        setHeaderIfAbsent(headers, HeaderConstants.STRICT_TRANSPORT_SECURITY,
                "max-age=31536000; includeSubDomains; preload");
        setHeaderIfAbsent(headers, HeaderConstants.CONTENT_SECURITY_POLICY, "default-src 'self'");
        setHeaderIfAbsent(headers, HeaderConstants.X_XSS_PROTECTION, "1; mode=block");
        setHeaderIfAbsent(headers, HeaderConstants.REFERRER_POLICY, "strict-origin-when-cross-origin");

        return chain.filter(exchange);
    }

    /**
     * Determines the execution order of this filter.
     * 
     * @return the order value (-10), ensuring it runs relatively early in the chain.
     */
    @Override
    public int getOrder() {
        return -10;
    }

    private static void setHeaderIfAbsent(HttpHeaders headers, String name, String value) {
        if (!headers.containsKey(name)) {
            headers.set(name, value);
        }
    }
}
