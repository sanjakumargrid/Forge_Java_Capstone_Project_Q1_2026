package com.talentgrid.kafka.producer;

import com.talentgrid.shared.event.BaseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;


    public void sendEvent(String topic, BaseEvent<?> event) {
        log.info("[KAFKA-PRODUCER] Sending event | type={} | eventId={} | topic={} | source={}",
                event.getEventType(), event.getEventId(), topic, event.getSource());

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(topic, event.getEventType(), event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("[KAFKA-PRODUCER] Failed to send event | type={} | eventId={} | error={}",
                        event.getEventType(), event.getEventId(), ex.getMessage(), ex);
                // TODO: Integrate with talentgrid-observability to emit failure metric
                // TODO: Consider persisting to an outbox / dead-letter store for retry
            } else {
                log.debug("[KAFKA-PRODUCER] Event delivered | type={} | partition={} | offset={}",
                        event.getEventType(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }
}
