package com.talentgrid.auth.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Service responsible for managing the JWT blacklist in Redis.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Store revoked JWT identifiers (JTIs) in Redis upon logout</li>
 *   <li>Check if an incoming JWT is blacklisted during authentication</li>
 *   <li>Ensure blacklisted tokens expire automatically when the token
 *       itself would have naturally expired</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class JwtBlacklistService {

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * Adds a JWT identifier (JTI) to the blacklist cache.
     *
     * <p>If the provided expiration time is zero or negative, the token
     * has already naturally expired and is not added to the blacklist
     * to save Redis memory and avoid {@link IllegalArgumentException}.
     *
     * @param jti              the unique JWT ID to blacklist
     * @param expirationMillis the remaining validity of the token in milliseconds
     */
    public void blacklistToken(
            String jti,
            long expirationMillis
    ) {
        if (expirationMillis <= 0) {
            // Token already expired naturally, no need to blacklist
            return;
        }

        redisTemplate.opsForValue().set(
                "blacklist:" + jti,
                "blacklisted",
                Duration.ofMillis(expirationMillis)
        );
    }

    /**
     * Checks if a JWT identifier (JTI) is present in the blacklist.
     *
     * @param jti the JWT ID to check
     * @return {@code true} if the token is blacklisted and invalid
     */
    public boolean isBlacklisted(String jti) {

        return Boolean.TRUE.equals(
                redisTemplate.hasKey("blacklist:" + jti)
        );
    }
}