package com.talentgrid.kafka.producer;

import com.talentgrid.shared.event.BaseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Generic Kafka producer for TalentGrid microservices.
 *
 * <p>All domain services should inject and use this bean instead of wiring
 * {@link KafkaTemplate} directly. This keeps producing logic consistent
 * (keying strategy, logging, error handling) across the entire system.</p>
 *
 * <h3>Keying strategy</h3>
 * <p>Messages are keyed by {@link BaseEvent#getEventType()} (e.g. {@code "DEMAND_CREATED"}).
 * This guarantees that all events of the same type are routed to the same partition,
 * preserving order for a given event type within each topic.</p>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * @Autowired
 * private KafkaProducerService kafkaProducerService;
 *
 * kafkaProducerService.sendEvent(TalentGridTopics.DEMAND_EVENTS, event);
 * }</pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Sends a {@link BaseEvent} to the specified Kafka topic asynchronously.
     *
     * <p>The message key is set to {@code event.getEventType()} to ensure
     * partition affinity for each event type.</p>
     *
     * @param topic the target Kafka topic (use constants from {@link com.talentgrid.kafka.topics.TalentGridTopics})
     * @param event the event envelope wrapping the business payload
     */
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
