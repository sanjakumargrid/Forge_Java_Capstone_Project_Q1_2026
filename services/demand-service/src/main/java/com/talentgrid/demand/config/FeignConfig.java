package com.talentgrid.demand.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Configuration class for OpenFeign clients.
 *
 * <p>Acts as a placeholder for centralized Feign configuration such as custom ErrorDecoders,
 * RequestInterceptors (e.g., for JWT token propagation), or specific Logger levels
 * across all inter-service communications.
 */
@Configuration
public class FeignConfig {

    /**
     * Forwards the inbound {@code Authorization} header when present (HTTP request thread only).
     * Background jobs without a request context behave as before (no header).
     */
    @Bean
    public RequestInterceptor forwardAuthorizationHeader() {
        return requestTemplate -> {
            var attrs = RequestContextHolder.getRequestAttributes();
            if (attrs instanceof ServletRequestAttributes servletRequestAttributes) {
                String auth = servletRequestAttributes.getRequest().getHeader("Authorization");
                if (auth != null && !auth.isBlank()) {
                    requestTemplate.header("Authorization", auth);
                }
            }
        };
    }
}
