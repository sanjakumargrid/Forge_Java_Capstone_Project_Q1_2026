package com.talentgrid.gateway.ratelimit;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

import com.talentgrid.gateway.constants.HeaderConstants;

import java.net.InetSocketAddress;

@Configuration
public class RedisRateLimitConfig {

    @Value("${ratelimit.default.replenishRate:30}")
    private int replenishRate;

    @Value("${ratelimit.default.burstCapacity:60}")
    private int burstCapacity;

    @Value("${ratelimit.default.requestedTokens:1}")
    private int requestedTokens;

    @Bean
    public RedisRateLimiter defaultRedisRateLimiter() {
        return new RedisRateLimiter(replenishRate, burstCapacity, requestedTokens);
    }

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
