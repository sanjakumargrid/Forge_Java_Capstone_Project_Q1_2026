package com.talentgrid.kafka.consumer;

import com.talentgrid.kafka.events.base.BaseEvent;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class BaseKafkaConsumer<T> {

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
    protected abstract void handleEvent(BaseEvent<T> event);
}
