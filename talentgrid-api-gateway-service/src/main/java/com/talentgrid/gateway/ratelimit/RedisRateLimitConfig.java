package com.talentgrid.gateway.ratelimit;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

import com.talentgrid.gateway.constants.HeaderConstants;

import java.net.InetSocketAddress;

/**
 * Configuration class for resolving rate-limit keys in Spring Cloud Gateway.
 * 
 * <p>Token bucket parameters (replenish rate, burst capacity) are configured externally
 * in {@code application.yml} via {@code spring.cloud.gateway.default-filters} and apply
 * to the auto-configured {@link org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter}.</p>
 * 
 * <p>Note: A custom {@code RedisRateLimiter} bean is intentionally omitted to avoid
 * disrupting the underlying Reactive Redis Lua script initialization.</p>
 */
@Configuration
public class RedisRateLimitConfig {

    /**
     * Provides the primary strategy for identifying uniquely rate-limited clients.
     * 
     * <p>If the request is authenticated, the client is uniquely identified by their 
     * {@code X-User-Id}. If unauthenticated (e.g., public paths), it falls back to 
     * the client's remote IP address. This ensures fair usage across both registered
     * users and anonymous consumers.</p>
     *
     * @return a {@link KeyResolver} emitting the resolved string key asynchronously
     */
    @Bean("userKeyResolver")
    @Primary
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst(HeaderConstants.USER_ID);
            if (userId != null && !userId.isBlank()) {
                return Mono.just("user:" + userId);
            }
            InetSocketAddress remoteAddr = exchange.getRequest().getRemoteAddress();
            String ip = (remoteAddr != null && remoteAddr.getAddress() != null)
                    ? remoteAddr.getAddress().getHostAddress()
                    : "unknown";
            return Mono.just("ip:" + ip);
        };
    }
}
