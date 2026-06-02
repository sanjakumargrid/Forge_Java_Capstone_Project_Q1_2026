package com.talentgrid.kafka.consumer;

import com.talentgrid.shared.event.BaseEvent;
import lombok.extern.slf4j.Slf4j;
// FIX #10: Removed unused import org.apache.kafka.clients.consumer.ConsumerRecord

/**
 * Abstract base class for all TalentGrid Kafka consumers.
 *
 * <p>Domain services extend this class and implement {@link #handleEvent(BaseEvent)}
 * to process incoming events. This base class centralises:
 * <ul>
 *   <li>Structured logging with MDC-compatible fields</li>
 *   <li>Error handling and dead-letter routing (placeholder)</li>
 *   <li>Consistent exception suppression so a bad message never kills the consumer thread</li>
 * </ul>
 * </p>
 *
 * <h3>How to implement a consumer in your service</h3>
 * <pre>{@code
 * @Service
 * public class DemandEventConsumer extends BaseKafkaConsumer<DemandPayload> {
 *
 *     @KafkaListener(topics = TalentGridTopics.DEMAND_EVENTS, groupId = "${spring.kafka.consumer.group-id}")
 *     public void listen(BaseEvent<DemandPayload> event) {
 *         process(event);
 *     }
 *
 *     @Override
 *     protected void handleEvent(BaseEvent<DemandPayload> event) {
 *         // Your business logic here
 *     }
 * }
 * }</pre>
 *
 * @param <T> The payload type expected in the event body
 */
@Slf4j
public abstract class BaseKafkaConsumer<T> {

    /**
     * Entry point for all consumer subclasses. Wraps {@link #handleEvent(BaseEvent)}
     * with structured logging and safe error handling.
     *
     * @param event the deserialized event from Kafka
     */
    protected void process(BaseEvent<T> event) {
        log.info("[KAFKA-CONSUMER] Received event | type={} | eventId={} | source={} | correlationId={}",
                event.getEventType(), event.getEventId(), event.getSource(), event.getCorrelationId());
        try {
            handleEvent(event);
            log.debug("[KAFKA-CONSUMER] Processed event | type={} | eventId={}",
                    event.getEventType(), event.getEventId());
        } catch (Exception e) {
            log.error("[KAFKA-CONSUMER] Error processing event | type={} | eventId={} | error={}",
                    event.getEventType(), event.getEventId(), e.getMessage(), e);
            // TODO: Route to dead-letter topic via talentgrid-kafka DLT support
            // TODO: Emit failure metric via talentgrid-observability
        }
    }

    /**
     * Implement this in your service to handle the business logic for a received event.
     *
     * @param event the event to handle
     */
    protected abstract void handleEvent(BaseEvent<T> event);
}