package com.talentgrid.auth.kafka;

import com.talentgrid.shared.kafka.AuthUserUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthUserEventPublisher {

    private final KafkaTemplate<String, AuthUserUpdatedEvent> kafkaTemplate;

    public void publishUserUpdated(AuthUserUpdatedEvent event) {
        kafkaTemplate.send("auth.user.updated", String.valueOf(event.getUserId()), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish auth update event for userId={}: {}",
                                event.getUserId(), ex.getMessage());
                    } else {
                        log.info("Published auth update event for userId={}", event.getUserId());
                    }
                });
    }

}