package com.talentgrid.workforce.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talentgrid.kafka.consumer.BaseKafkaConsumer;
import com.talentgrid.kafka.events.base.BaseEvent;
import com.talentgrid.kafka.topics.TalentGridTopics;
import com.talentgrid.workforce.airmgnomination.services.ResumeParserService;
import com.talentgrid.workforce.engineerprofilemanagement.dto.EmployeeProfileUpdatedPayload;
import com.talentgrid.workforce.engineerprofilemanagement.dto.InternalEmployeeResponse;
import com.talentgrid.workforce.engineerprofilemanagement.dto.UserDto;
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
    private final ResumeParserService resumeParserService;

    @KafkaListener(topics = {TalentGridTopics.WORKFORCE_EVENTS, TalentGridTopics.AUTH_USER_CREATED}, groupId = "${spring.kafka.consumer.group-id}")
    public void consume(BaseEvent<Map<String, Object>> event) {
        process(event);
    }

    @Override
    protected void handleEvent(BaseEvent<Map<String, Object>> event) {
        switch (event.getEventType()) {
            case "USER_CREATED" -> handleUserCreated(event);
            case "EMPLOYEE_PROFILE_UPDATED" -> handleEngineerResumeUpdated(event);
            default -> log.warn(
                    "[WORKFORCE-CONSUMER] Unsupported event type={} eventId={}",
                    event.getEventType(),
                    event.getEventId()
            );
        }
    }

    private void syncInternalEmployee(BaseEvent<Map<String, Object>> event) {
        Map<String, Object> payload = event.getPayload();
        UserDto userDto;

        if (payload != null && (payload.containsKey("userId") || payload.containsKey("username"))) {
            userDto = new UserDto();
            Number userIdNum = (Number) payload.get("userId");
            userDto.setEmployeeId(userIdNum != null ? userIdNum.longValue() : null);
            userDto.setEmail((String) payload.get("email"));
            userDto.setName((String) payload.get("username"));
            userDto.setLocation((String) payload.get("location"));
            userDto.setIsActive(true);
        } else {
            userDto = objectMapper.convertValue(payload, UserDto.class);
        }

        InternalEmployeeResponse response = internalEmployeeService.syncEmployeeFromKafka(userDto);

        log.info("[WORKFORCE-CONSUMER] Internal employee synced | eventType={} | eventId={} | employeeId={} | email={}",
                event.getEventType(), event.getEventId(), response.getEmployeeId(), response.getEmail());
    }

    private void handleUserCreated(BaseEvent<Map<String, Object>> event) {
        syncInternalEmployee(event);
    }

    private void handleEngineerResumeUpdated(BaseEvent<Map<String, Object>> event) {
        EmployeeProfileUpdatedPayload payload = objectMapper.convertValue(event.getPayload(), EmployeeProfileUpdatedPayload.class);

        if (payload == null || payload.getEmployeeId() == null) {
            log.warn("[WORKFORCE-CONSUMER] Invalid employee profile update payload | eventId={}", event.getEventId());
            return;
        }

        if (payload.getResumeDriveLink() == null || payload.getResumeDriveLink().isBlank()) {
            log.info("[WORKFORCE-CONSUMER] Profile update without resume link ignored for parsing | eventId={} | employeeId={}",
                    event.getEventId(), payload.getEmployeeId());
            return;
        }

        try {
            String pastedText = resumeParserService.parseResumeText(payload);
            log.info("[WORKFORCE-CONSUMER] Resume parsed | eventId={} | employeeId={} | textLength={}",
                    event.getEventId(), payload.getEmployeeId(), pastedText.length());
        } catch (Exception ex) {
            log.error("[WORKFORCE-CONSUMER] Resume parsing failed | eventId={} | employeeId={} | resumeDriveLink={} | error={}",
                    event.getEventId(), payload.getEmployeeId(), payload.getResumeDriveLink(), ex.getMessage(), ex);
            throw ex;
        }
    }

}
