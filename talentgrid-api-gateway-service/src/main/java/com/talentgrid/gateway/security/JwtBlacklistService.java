package com.talentgrid.gateway.security;

import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import com.talentgrid.gateway.constants.AppConstants;

import org.springframework.beans.factory.annotation.Qualifier;

@Service
public class JwtBlacklistService {

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    public JwtBlacklistService(
            @Qualifier("reactiveRedisTemplate") ReactiveRedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Checks if a token ID (JTI) is present in the Redis blacklist.
     * The Auth service sets the key "blacklist:<jti>" when a user logs out.
     */
    public Mono<Boolean> isBlacklisted(String jti) {
        if (jti == null) {
            return Mono.just(false);
        }
        return redisTemplate.hasKey(AppConstants.REDIS_BLACKLIST_PREFIX + jti)
                .defaultIfEmpty(false);
    }
}
