package com.talentgrid.demand.kafka;

import com.talentgrid.kafka.events.demand.DemandPayload;
import com.talentgrid.kafka.events.base.BaseEvent;
import com.talentgrid.kafka.producer.KafkaProducerService;
import com.talentgrid.kafka.topics.TalentGridTopics;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Demand-service Kafka producer.
 *
 * <p>Delegates to the shared {@link KafkaProducerService}. Domain services
 * should create a thin wrapper like this rather than calling the shared
 * producer directly — it keeps event construction logic co-located with
 * the domain that owns it.</p>
 */
@Component
@RequiredArgsConstructor
public class DemandKafkaProducer {

    private final KafkaProducerService kafkaProducerService;

    /**
     * Publishes a {@code DEMAND_CREATED} event to {@code demand-events}.
     *
     * @param payload    the demand details
     * @param requestId  the correlation ID from the HTTP request (from MDC / X-Request-Id header)
     */
    public void publishDemandCreated(DemandPayload payload, String requestId) {
        BaseEvent<DemandPayload> event = BaseEvent.<DemandPayload>builder()
                .eventType("DEMAND_CREATED")
                .source("demand-service")
                .correlationId(requestId != null ? requestId : UUID.randomUUID().toString())
                .payload(payload)
                .build();

        kafkaProducerService.sendEvent(TalentGridTopics.DEMAND_EVENTS, event);
    }

    /**
     * Publishes a {@code DEMAND_APPROVED} event to {@code demand-events}.
     */
    public void publishDemandApproved(DemandPayload payload, String requestId) {
        BaseEvent<DemandPayload> event = BaseEvent.<DemandPayload>builder()
                .eventType("DEMAND_APPROVED")
                .source("demand-service")
                .correlationId(requestId != null ? requestId : UUID.randomUUID().toString())
                .payload(payload)
                .build();

        kafkaProducerService.sendEvent(TalentGridTopics.DEMAND_EVENTS, event);
    }
}
