package com.talentgrid.workforce.hmapproval.kafka;

import com.talentgrid.kafka.events.base.BaseEvent;
import com.talentgrid.kafka.producer.KafkaProducerService;
import com.talentgrid.kafka.topics.TalentGridTopics;
import com.talentgrid.workforce.hmapproval.dto.MatchAcceptedPayload;
import com.talentgrid.workforce.hmapproval.dto.MatchRejectedPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class HmApprovalKafkaProducer {

    private final KafkaProducerService kafkaProducerService;

    public void publishMatchAccepted(MatchAcceptedPayload payload) {
        BaseEvent<MatchAcceptedPayload> event = new BaseEvent<>();
        event.setEventType("internal_match.accepted");
        event.setSource("workforce-service");
        event.setCorrelationId(UUID.randomUUID().toString());
        event.setPayload(payload);
        kafkaProducerService.sendEvent(TalentGridTopics.WORKFORCE_EVENTS, UUID.randomUUID().toString(), event);
    }

    public void publishMatchRejected(MatchRejectedPayload payload) {
        BaseEvent<MatchRejectedPayload> event = new BaseEvent<>();
        event.setEventType("internal_match.rejected");
        event.setSource("workforce-service");
        event.setCorrelationId(UUID.randomUUID().toString());
        event.setPayload(payload);
        kafkaProducerService.sendEvent(TalentGridTopics.WORKFORCE_EVENTS, UUID.randomUUID().toString(), event);
    }
}
