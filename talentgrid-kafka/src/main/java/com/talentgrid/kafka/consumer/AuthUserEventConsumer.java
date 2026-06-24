package com.talentgrid.kafka.consumer;

import com.talentgrid.kafka.events.auth.AuthUserPayload;
import com.talentgrid.kafka.events.base.BaseEvent;
import com.talentgrid.kafka.serializer.EventDeserializer;
import com.talentgrid.kafka.topics.TalentGridTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnBean(name = "objectRedisTemplate")
@ConditionalOnProperty(name = "talentgrid.kafka.auth-consumer.enabled", havingValue = "true", matchIfMissing = false)
public class AuthUserEventConsumer extends BaseKafkaConsumer<AuthUserPayload> {

    private final RedisTemplate<String, Object> objectRedisTemplate;
    private final EventDeserializer eventDeserializer;

    @KafkaListener(
            topics = TalentGridTopics.AUTH_USER_UPDATED,
            groupId = "${spring.application.name}",
            containerFactory = "authUserKafkaListenerContainerFactory"  // 👈 use dedicated factory
    )
    public void onUserUpdated(String rawMessage) {
        // now rawMessage is actually a String — works correctly
        try {
            BaseEvent<AuthUserPayload> event =
                    eventDeserializer.deserialize(rawMessage, AuthUserPayload.class);
            process(event);
        } catch (Exception e) {
            log.error("Failed to deserialize AUTH_USER_UPDATED event: {}", e.getMessage());
        }
    }

    @Override
    protected void handleEvent(BaseEvent<AuthUserPayload> event) {
        AuthUserPayload payload = event.getPayload();

        if (payload == null || payload.getUserId() == null) {
            log.warn("Received invalid AuthUserPayload, skipping.");
            return;
        }

        String key = "auth:user:" + payload.getUserId();
        objectRedisTemplate.delete(key);

        log.info("Evicted Redis cache for userId={} due to auth update event.",
                payload.getUserId());
    }
}