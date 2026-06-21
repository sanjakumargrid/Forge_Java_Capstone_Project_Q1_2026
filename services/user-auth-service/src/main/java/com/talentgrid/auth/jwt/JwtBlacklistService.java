package com.talentgrid.auth.jwt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtBlacklistService {

    private final RedisTemplate<String, String> redisTemplate;

    public void blacklistToken(
            String jti,
            long expirationMillis
    ) {
        if (expirationMillis <= 0) {
            return; // already expired, nothing to blacklist
        }
        try {
            redisTemplate.opsForValue().set(
                    "blacklist:" + jti,
                    "blacklisted",
                    Duration.ofMillis(expirationMillis)
            );
        } catch (Exception ex) {
            // Redis unavailable (e.g. not running locally) — don't fail logout.
            log.warn("Unable to blacklist token (Redis unavailable): {}", ex.getMessage());
        }
    }

    public boolean isBlacklisted(String jti) {
        try {
            return Boolean.TRUE.equals(
                    redisTemplate.hasKey("blacklist:" + jti)
            );
        } catch (Exception ex) {
            // Redis is not running locally, assume token is not blacklisted
            return false;
        }
    }
}