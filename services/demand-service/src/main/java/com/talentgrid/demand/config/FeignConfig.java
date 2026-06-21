package com.talentgrid.demand.config;

import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for OpenFeign clients.
 *
 * <p>Acts as a placeholder for centralized Feign configuration such as custom ErrorDecoders,
 * RequestInterceptors (e.g., for JWT token propagation), or specific Logger levels
 * across all inter-service communications.
 *
 * <p>JWT propagation for user-auth-service is handled in {@link UserAuthFeignConfig}.
 */
@Configuration
public class FeignConfig {
}
