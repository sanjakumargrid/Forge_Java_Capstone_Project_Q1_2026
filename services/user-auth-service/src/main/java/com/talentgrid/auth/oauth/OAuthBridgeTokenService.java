package com.talentgrid.auth.oauth;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;

/**
 * Short-lived bridge tokens that carry an OAuth exchange code across a gateway redirect
 * so the HttpOnly cookie can be set on a dedicated same-origin API request.
 */
@Service
@RequiredArgsConstructor
public class OAuthBridgeTokenService {

    private static final String KEY_PREFIX = "auth:oauth:bridge:";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RedisTemplate<String, String> redisTemplate;

    public String createBridgeToken(String exchangeCode, long ttlSeconds) {
        String bridgeToken = generateSecureToken();
        redisTemplate.opsForValue().set(
                KEY_PREFIX + bridgeToken,
                exchangeCode,
                Duration.ofSeconds(ttlSeconds)
        );
        return bridgeToken;
    }

    public Optional<String> consumeBridgeToken(String bridgeToken) {
        if (bridgeToken == null || bridgeToken.isBlank()) {
            return Optional.empty();
        }

        String exchangeCode = redisTemplate.opsForValue().getAndDelete(KEY_PREFIX + bridgeToken);
        if (exchangeCode == null || exchangeCode.isBlank()) {
            return Optional.empty();
        }

        return Optional.of(exchangeCode);
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
