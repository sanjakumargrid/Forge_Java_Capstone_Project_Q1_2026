package com.talentgrid.auth.kafka;

import com.talentgrid.kafka.events.auth.AuthUserPayload;
import com.talentgrid.kafka.events.base.BaseEvent;
import com.talentgrid.kafka.producer.KafkaProducerService;
import com.talentgrid.kafka.topics.TalentGridTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthUserEventPublisher {

    private final KafkaProducerService kafkaProducerService;

    public void publishUserUpdated(AuthUserPayload payload) {

        BaseEvent<AuthUserPayload> event = BaseEvent.<AuthUserPayload>builder()
                .eventType("AUTH_USER_UPDATED")
                .source("user-auth-service")
                .payload(payload)
                .build();

        kafkaProducerService.sendEvent(
                TalentGridTopics.AUTH_USER_UPDATED,
                String.valueOf(payload.getUserId()),
                event
        );
    }
}