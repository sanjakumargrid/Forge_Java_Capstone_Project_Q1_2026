package com.talentgrid.shared.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "redis.enabled",
        havingValue = "true"
)
public class AuthUserEventConsumer {

    private final RedisTemplate<String, Object> objectRedisTemplate;

    @KafkaListener(
            topics = "auth.user.updated",
            groupId = "${spring.application.name}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onUserUpdated(AuthUserUpdatedEvent event) {

        if (event == null || event.getUserId() == null) {
            log.warn("Received null or invalid AuthUserUpdatedEvent, skipping.");
            return;
        }

        // Evict the user's cache — next request will fail authVersion check
        // and force re-login
        String key = "auth:user:" + event.getUserId();
        objectRedisTemplate.delete(key);

        log.info("Evicted Redis cache for userId={} due to auth update event.",
                event.getUserId());
    }
}