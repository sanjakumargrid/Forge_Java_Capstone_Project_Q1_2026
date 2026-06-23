package com.talentgrid.auth.kafka;

import com.talentgrid.kafka.events.auth.UserCreatedPayload;
import com.talentgrid.kafka.events.base.BaseEvent;
import com.talentgrid.kafka.producer.KafkaProducerService;
import com.talentgrid.kafka.topics.TalentGridTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserCreatedEventPublisher {

    private final KafkaProducerService kafkaProducerService;

    public void publishUserCreated(UserCreatedPayload payload) {

        BaseEvent<UserCreatedPayload> event = BaseEvent.<UserCreatedPayload>builder()
                .eventType("USER_CREATED")
                .source("user-auth-service")
                .payload(payload)
                .build();

        kafkaProducerService.sendEvent(
                TalentGridTopics.AUTH_USER_CREATED,
                String.valueOf(payload.getUserId()),
                event
        );
    }
}