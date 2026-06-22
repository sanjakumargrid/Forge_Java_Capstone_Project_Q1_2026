package com.talentgrid.gateway.security;

import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import com.talentgrid.gateway.constants.AppConstants;

import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Service responsible for checking the revocation status of JSON Web Tokens.
 * 
 * <p>Interfaces with a reactive Redis store to determine if a given JWT identifier (JTI)
 * has been blacklisted. The User Authentication service populates this blacklist when 
 * tokens are manually revoked or when users log out, ensuring immediate invalidation 
 * across the cluster prior to natural token expiration.</p>
 */
@Service
public class JwtBlacklistService {

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    /**
     * Constructs a new {@code JwtBlacklistService}.
     *
     * @param redisTemplate the reactive Redis template for asynchronous lookups
     */
    public JwtBlacklistService(
            @Qualifier("reactiveRedisTemplate") ReactiveRedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Checks if a token ID (JTI) is present in the Redis blacklist.
     * 
     * <p>The presence of the key {@code blacklist:<jti>} indicates the token
     * has been revoked.</p>
     * 
     * @param jti the unique identifier of the JWT
     * @return a {@link Mono} emitting {@code true} if blacklisted, {@code false} otherwise
     */
    public Mono<Boolean> isBlacklisted(String jti) {
        if (jti == null) {
            return Mono.just(false);
        }
        return redisTemplate.hasKey(AppConstants.REDIS_BLACKLIST_PREFIX + jti)
                .defaultIfEmpty(false);
    }
}
