package com.talentgrid.workforce.engineerprofilemanagement.kafka.producer;

import com.talentgrid.kafka.producer.KafkaProducerService;
import com.talentgrid.kafka.topics.TalentGridTopics;
import com.talentgrid.shared.event.BaseEvent;
import com.talentgrid.workforce.engineerprofilemanagement.dto.EmployeeProfileUpdatedPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class WorkforceKafkaProducer {

    private final KafkaProducerService kafkaProducerService;

    public void publishEmployeeProfileUpdated(EmployeeProfileUpdatedPayload payload, String requestId) {
        BaseEvent<EmployeeProfileUpdatedPayload> event = new BaseEvent<>();

        event.setEventType("employee.profile_updated");
        event.setSource("workforce-service");

        if (requestId != null) {
            event.setCorrelationId(requestId);
        } else {
            event.setCorrelationId(UUID.randomUUID().toString());
        }

        event.setPayload(Objects.requireNonNull(payload, "Payload cannot be null"));
        kafkaProducerService.sendEvent(TalentGridTopics.WORKFORCE_EVENTS, event);
    }
}