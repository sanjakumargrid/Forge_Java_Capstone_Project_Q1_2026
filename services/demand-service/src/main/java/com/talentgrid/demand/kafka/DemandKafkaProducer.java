package com.talentgrid.demand.kafka;

import com.talentgrid.demand.constants.DemandConstants;
import com.talentgrid.kafka.events.base.BaseEvent;
import com.talentgrid.kafka.events.demand.DemandPayload;
import com.talentgrid.kafka.producer.KafkaProducerService;
import com.talentgrid.kafka.topics.TalentGridTopics;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DemandKafkaProducer {

    private final KafkaProducerService kafkaProducerService;

    public void publishDemandCreated(
            DemandPayload payload,
            String requestId
    ) {

        String correlationId =
                requestId != null
                        ? requestId
                        : UUID.randomUUID().toString();

        BaseEvent<DemandPayload> event =
                BaseEvent.<DemandPayload>builder()
                        .eventType(
                                DemandConstants.DEMAND_CREATED_EVENT
                        )
                        .source(
                                DemandConstants.SERVICE_NAME
                        )
                        .correlationId(
                                correlationId
                        )
                        .payload(
                                payload
                        )
                        .build();

        kafkaProducerService.sendEvent(
                TalentGridTopics.DEMAND_EVENTS,
                correlationId,
                event
        );
    }

    public void publishDemandApproved(
            DemandPayload payload,
            String requestId
    ) {

        String correlationId =
                requestId != null
                        ? requestId
                        : UUID.randomUUID().toString();

        BaseEvent<DemandPayload> event =
                BaseEvent.<DemandPayload>builder()
                        .eventType(
                                DemandConstants.DEMAND_APPROVED_EVENT
                        )
                        .source(
                                DemandConstants.SERVICE_NAME
                        )
                        .correlationId(
                                correlationId
                        )
                        .payload(
                                payload
                        )
                        .build();

        kafkaProducerService.sendEvent(
                TalentGridTopics.DEMAND_EVENTS,
                correlationId,
                event
        );
    }
}