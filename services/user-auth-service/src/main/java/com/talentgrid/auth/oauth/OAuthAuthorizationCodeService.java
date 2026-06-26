package com.talentgrid.auth.oauth;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;

/**
 * Stores short-lived, one-time OAuth exchange codes in Redis.
 *
 * <p>After Google login succeeds, the exchange code is placed in an HttpOnly cookie.
 * The SPA exchanges it for tokens via {@code POST /api/v1/auth/oauth/token}.
 */
@Service
@RequiredArgsConstructor
public class OAuthAuthorizationCodeService {

    private static final String KEY_PREFIX = "auth:oauth:code:";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * Creates a one-time exchange code bound to the authenticated user id.
     *
     * @param userId      database user id
     * @param ttlSeconds  code lifetime in seconds
     * @return opaque exchange code
     */
    public String createCode(Long userId, long ttlSeconds) {
        String code = generateSecureCode();
        redisTemplate.opsForValue().set(
                KEY_PREFIX + code,
                String.valueOf(userId),
                Duration.ofSeconds(ttlSeconds)
        );
        return code;
    }

    private String generateSecureCode() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Consumes a one-time exchange code and returns the associated user id.
     *
     * @param code exchange code from the OAuth redirect
     * @return user id if the code was valid and not yet consumed
     */
    public Optional<Long> consumeCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }

        String key = KEY_PREFIX + code;
        String userId = redisTemplate.opsForValue().getAndDelete(key);
        if (userId == null) {
            return Optional.empty();
        }

        return Optional.of(Long.valueOf(userId));
    }
}
