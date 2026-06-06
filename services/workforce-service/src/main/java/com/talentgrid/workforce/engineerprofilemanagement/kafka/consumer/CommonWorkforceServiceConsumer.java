package com.talentgrid.workforce.engineerprofilemanagement.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talentgrid.kafka.consumer.BaseKafkaConsumer;
import com.talentgrid.kafka.topics.TalentGridTopics;
import com.talentgrid.shared.event.BaseEvent;
import com.talentgrid.workforce.engineerprofilemanagement.dto.InternalEmployeeResponse;
import com.talentgrid.workforce.engineerprofilemanagement.kafka.producer.UserDto;
import com.talentgrid.workforce.engineerprofilemanagement.service.InternalEmployeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommonWorkforceServiceConsumer extends BaseKafkaConsumer<Map<String, Object>> {

    private final ObjectMapper objectMapper;
    private final InternalEmployeeService internalEmployeeService;

    @KafkaListener(topics = TalentGridTopics.WORKFORCE_EVENTS, groupId = "${spring.kafka.consumer.group-id}")
    public void consume(BaseEvent<Map<String, Object>> event) {
        process(event);
    }

    @Override
    protected void handleEvent(BaseEvent<Map<String, Object>> event) {
        switch (event.getEventType()) {
            case "USER_CREATED" -> handleUserCreated(event);
            default -> log.warn(
                    "[WORKFORCE-CONSUMER] Unsupported event type={} eventId={}",
                    event.getEventType(),
                    event.getEventId()
            );
        }
    }

    private void syncInternalEmployee(BaseEvent<Map<String, Object>> event) {
        UserDto userDto = objectMapper.convertValue(event.getPayload(), UserDto.class);
        InternalEmployeeResponse response = internalEmployeeService.syncEmployeeFromKafka(userDto);

        log.info("[WORKFORCE-CONSUMER] Internal employee synced | eventType={} | eventId={} | employeeId={} | email={}",
                event.getEventType(), event.getEventId(), response.getEmployeeId(), response.getEmail());
    }

    private void handleUserCreated(BaseEvent<Map<String, Object>> event) {
        syncInternalEmployee(event);
    }

}
