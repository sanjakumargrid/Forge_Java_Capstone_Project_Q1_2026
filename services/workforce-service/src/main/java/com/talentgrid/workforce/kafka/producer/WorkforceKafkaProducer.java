package com.talentgrid.workforce.kafka.producer;

import com.talentgrid.kafka.producer.KafkaProducerService;
import com.talentgrid.kafka.topics.TalentGridTopics;
import com.talentgrid.kafka.events.base.BaseEvent;
import com.talentgrid.workforce.engineerprofilemanagement.dto.EmployeeImportedPayload;
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
        String key = UUID.randomUUID().toString();
        event.setEventType("employee.profile_updated");
        event.setSource("workforce-service");

        if (requestId != null) {
            event.setCorrelationId(requestId);
        } else {
            event.setCorrelationId(UUID.randomUUID().toString());
        }

        event.setPayload(Objects.requireNonNull(payload, "Payload cannot be null"));
        kafkaProducerService.sendEvent(TalentGridTopics.WORKFORCE_EVENTS,key,event);
    }

    public void publishEmployeeImported(EmployeeImportedPayload payload, String requestId) {
        BaseEvent<EmployeeImportedPayload> event = new BaseEvent<>();
        String key = UUID.randomUUID().toString();
        event.setEventType("employee.imported");
        event.setSource("workforce-service");
        if (requestId != null) {
            event.setCorrelationId(requestId);
        } else {
            event.setCorrelationId(UUID.randomUUID().toString());
        }

        event.setPayload(Objects.requireNonNull(payload, "Payload cannot be null"));
        kafkaProducerService.sendEvent(TalentGridTopics.WORKFORCE_EVENTS, key, event);
    }
}