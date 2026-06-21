package com.talentgrid.auth.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talentgrid.auth.dto.cache.CachedUserContext;
import com.talentgrid.auth.entity.Role;
import com.talentgrid.auth.entity.Scope;
import com.talentgrid.auth.entity.User;
import com.talentgrid.auth.service.interfaces.UserSecurityCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service to manage the user authorization context in Redis.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Cache user roles, scopes, and authVersion upon login</li>
 *   <li>Provide fast O(1) lookups during JWT validation</li>
 *   <li>Evict cache when required</li>
 * </ul>
 *
 * <p>The TTL is set to 24 hours. Since access tokens are short-lived (15 min)
 * and refresh tokens trigger a re-cache, 24h ensures the cache outlives
 * active sessions without consuming infinite memory. A cache miss results
 * in an automatic re-login prompt.
 */
@Service
@RequiredArgsConstructor
public class UserSecurityCacheServiceImpl
        implements UserSecurityCacheService {

    private static final String PREFIX =
            "auth:user:";

    private final RedisTemplate<String, Object>
            objectRedisTemplate;

    private final ObjectMapper objectMapper;

    @Override
    public void cacheUser(User user) {

        CachedUserContext context =
                CachedUserContext.builder()
                        .userId(user.getId())
                        .email(user.getEmail())
                        .enabled(user.getEnabled())
                        .authVersion(user.getAuthVersion())

                        .roles(
                                user.getRoles()
                                        .stream()
                                        .map(Role::getName)
                                        .collect(Collectors.toSet())
                        )

                        .scopes(
                                user.getRoles()
                                        .stream()
                                        .flatMap(role ->
                                                role.getScopes().stream()
                                        )
                                        .map(Scope::getName)
                                        .collect(Collectors.toSet())
                        )
                        .build();

        objectRedisTemplate.opsForValue().set(
                PREFIX + user.getId(),
                context,
                Duration.ofHours(24)
        );
    }

    @Override
    public CachedUserContext getUser(Long userId) {

        Object value =
                objectRedisTemplate.opsForValue()
                        .get(PREFIX + userId);

        if (value == null) {
            return null;
        }

        if (value instanceof CachedUserContext) {
            return (CachedUserContext) value;
        }

        return null;
    }

    @Override
    public void evictUser(Long userId) {

        objectRedisTemplate.delete(
                PREFIX + userId
        );
    }
}