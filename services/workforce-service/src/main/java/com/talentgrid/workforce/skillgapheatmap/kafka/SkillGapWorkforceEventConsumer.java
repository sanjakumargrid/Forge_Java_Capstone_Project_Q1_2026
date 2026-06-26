package com.talentgrid.workforce.skillgapheatmap.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talentgrid.kafka.consumer.BaseKafkaConsumer;
import com.talentgrid.kafka.events.base.BaseEvent;
import com.talentgrid.kafka.topics.TalentGridTopics;
import com.talentgrid.workforce.engineerprofilemanagement.dto.EmployeeProfileUpdatedPayload;
import com.talentgrid.workforce.skillgapheatmap.service.SkillGapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Refreshes bench skill counts when an engineer on bench updates skills or
 * availability. Uses a dedicated consumer group so profile events are not lost
 * to other workforce listeners on the same topic.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SkillGapWorkforceEventConsumer extends BaseKafkaConsumer<Map<String, Object>> {

    private static final String CONSUMER_GROUP = "workforce-skillgap-workforce-group";

    private final SkillGapService skillGapService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = TalentGridTopics.WORKFORCE_EVENTS,
            groupId = CONSUMER_GROUP
    )
    public void consume(BaseEvent<Map<String, Object>> event) {
        process(event);
    }

    @Override
    protected void handleEvent(BaseEvent<Map<String, Object>> event) {
        if (!"EMPLOYEE_PROFILE_UPDATED".equals(event.getEventType())) {
            return;
        }

        EmployeeProfileUpdatedPayload payload =
                objectMapper.convertValue(event.getPayload(), EmployeeProfileUpdatedPayload.class);

        if (payload == null || payload.getUpdatedFields() == null || payload.getUpdatedFields().isEmpty()) {
            return;
        }

        boolean benchRelevant = payload.getUpdatedFields().contains("skills")
                || payload.getUpdatedFields().contains("availabilityDate");

        if (!benchRelevant) {
            return;
        }

        log.info("[SKILL-GAP] Bench profile changed — refreshing snapshot | employeeId={} | fields={}",
                payload.getEmployeeId(), payload.getUpdatedFields());

        skillGapService.refreshBenchFromDatabase();
    }
}
