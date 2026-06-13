package com.talentgrid.workforce.kafka.consumer;

import com.talentgrid.kafka.consumer.BaseKafkaConsumer;
import com.talentgrid.kafka.events.base.BaseEvent;
import com.talentgrid.kafka.topics.TalentGridTopics;
import com.talentgrid.workforce.skillgapheatmap.service.SkillGapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * Listens on the {@code demand-events} Kafka topic and triggers a heatmap refresh
 * whenever the open demand pool changes.
 *
 * <p>Events that affect the heatmap:
 * <ul>
 *   <li>{@code DEMAND_CREATED}  — a new demand with skills has entered the system
 *   <li>{@code DEMAND_APPROVED} — demand transitions to INTERNAL_SEARCH (now open)
 *   <li>{@code DEMAND_CLOSED}   — demand is no longer open; reduces demand pressure
 *   <li>{@code DEMAND_CANCELLED}— demand removed from open pool
 * </ul>
 *
 * <p>The refresh is idempotent — calling it multiple times in quick succession
 * simply overwrites the previous snapshot with fresh data.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DemandEventConsumer extends BaseKafkaConsumer<Map<String, Object>> {

    /**
     * Demand event types that change the composition of the open demand pool
     * and therefore require a heatmap snapshot to be recalculated.
     */
    private static final Set<String> REFRESH_TRIGGER_EVENTS = Set.of(
            "DEMAND_CREATED",
            "DEMAND_APPROVED",
            "DEMAND_CLOSED",
            "DEMAND_CANCELLED"
    );

    private final SkillGapService skillGapService;

    @KafkaListener(
            topics = TalentGridTopics.DEMAND_EVENTS,
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(BaseEvent<Map<String, Object>> event) {
        process(event);
    }

    @Override
    protected void handleEvent(BaseEvent<Map<String, Object>> event) {
        String eventType = event.getEventType();

        if (!REFRESH_TRIGGER_EVENTS.contains(eventType)) {
            log.debug("[SKILL-GAP] Ignoring demand event type={} — not a refresh trigger.", eventType);
            return;
        }

        log.info("[SKILL-GAP] Demand event received — triggering heatmap refresh | type={} | eventId={}",
                eventType, event.getEventId());

        skillGapService.refresh();

        log.info("[SKILL-GAP] Heatmap refreshed successfully after demand event | type={} | eventId={}",
                eventType, event.getEventId());
    }
}
