package com.talentgrid.workforce.skillgapheatmap.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talentgrid.kafka.consumer.BaseKafkaConsumer;
import com.talentgrid.kafka.events.base.BaseEvent;
import com.talentgrid.kafka.events.demand.DemandPayload;
import com.talentgrid.kafka.topics.TalentGridTopics;
import com.talentgrid.workforce.skillgapheatmap.service.SkillGapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * Maintains the local active-demand registry and refreshes the heatmap snapshot
 * when demand lifecycle events change the open pool. Uses a dedicated consumer
 * group so every event is processed independently of other workforce listeners.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SkillGapDemandEventConsumer extends BaseKafkaConsumer<Map<String, Object>> {

    private static final String CONSUMER_GROUP = "workforce-skillgap-heatmap-group";

    private static final Set<String> ACTIVE_DEMAND_EVENTS = Set.of(
            "DEMAND_APPROVED",
            "DEMAND_EXTERNAL_OPENED",
            "DEMAND_RESUMED"
    );

    private static final Set<String> REMOVE_DEMAND_EVENTS = Set.of(
            "DEMAND_CLOSED",
            "DEMAND_FILLED",
            "DEMAND_ON_HOLD",
            "DEMAND_APPROVAL_SLA_CLOSED"
    );

    private final SkillGapService skillGapService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = TalentGridTopics.DEMAND_EVENTS,
            groupId = CONSUMER_GROUP
    )
    public void consume(BaseEvent<Map<String, Object>> event) {
        process(event);
    }

    @Override
    protected void handleEvent(BaseEvent<Map<String, Object>> event) {
        String eventType = event.getEventType();
        DemandPayload payload = objectMapper.convertValue(event.getPayload(), DemandPayload.class);

        if (payload == null || payload.getDemandId() == null) {
            log.warn("[SKILL-GAP] Demand event missing demandId | type={} | eventId={}",
                    eventType, event.getEventId());
            return;
        }

        if (ACTIVE_DEMAND_EVENTS.contains(eventType)) {
            log.info("[SKILL-GAP] Active demand entered | type={} | demandId={} | status={}",
                    eventType, payload.getDemandId(), payload.getStatus());
            skillGapService.onActiveDemandEntered(payload);
            return;
        }

        if (REMOVE_DEMAND_EVENTS.contains(eventType)) {
            log.info("[SKILL-GAP] Active demand removed | type={} | demandId={}",
                    eventType, payload.getDemandId());
            skillGapService.onActiveDemandRemoved(payload.getDemandId());
        }
    }
}
