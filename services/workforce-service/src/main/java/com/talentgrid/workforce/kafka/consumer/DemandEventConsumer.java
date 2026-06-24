package com.talentgrid.workforce.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talentgrid.kafka.consumer.BaseKafkaConsumer;
import com.talentgrid.kafka.events.base.BaseEvent;
import com.talentgrid.kafka.events.demand.DemandPayload;
import com.talentgrid.kafka.topics.TalentGridTopics;
import com.talentgrid.workforce.airmgnomination.services.DemandEmbeddingService;
import com.talentgrid.workforce.skillgapheatmap.service.SkillGapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * Listens on the {@code demand-events} Kafka topic and handles two concerns:
 *
 * <ol>
 *   <li><b>Skill-Gap Heatmap</b> — refreshes the snapshot whenever the open demand
 *       pool changes (CREATED, APPROVED, CLOSED, CANCELLED).</li>
 *   <li><b>Demand Embedding</b> — generates and persists a semantic embedding vector
 *       in {@code demand_embedding} when a demand is approved ({@code DEMAND_APPROVED}).
 *       This enables vector-similarity matching between open demands and engineer profiles.</li>
 * </ol>
 *
 * <p>Events that affect the heatmap:
 * <ul>
 *   <li>{@code DEMAND_CREATED}  — new demand with skills entered the system</li>
 *   <li>{@code DEMAND_APPROVED} — demand transitions to INTERNAL_SEARCH (now open)</li>
 *   <li>{@code DEMAND_CLOSED}   — demand no longer open; reduces demand pressure</li>
 *   <li>{@code DEMAND_CANCELLED}— demand removed from open pool</li>
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
    private final DemandEmbeddingService demandEmbeddingService;
    private final ObjectMapper objectMapper;

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

        // --- Skill-Gap Heatmap refresh ---
        if (REFRESH_TRIGGER_EVENTS.contains(eventType)) {
            log.info("[SKILL-GAP] Demand event received — triggering heatmap refresh | type={} | eventId={}",
                    eventType, event.getEventId());
            skillGapService.refresh();
            log.info("[SKILL-GAP] Heatmap refreshed successfully | type={} | eventId={}",
                    eventType, event.getEventId());
        } else {
            log.debug("[SKILL-GAP] Ignoring demand event type={} — not a refresh trigger.", eventType);
        }

        // --- Demand Embedding (only on DEMAND_APPROVED) ---
        if ("DEMAND_APPROVED".equals(eventType)) {
            handleDemandApprovedEmbedding(event);
        }
    }

    /**
     * Converts the raw event payload map to a {@link DemandPayload} and delegates
     * embedding generation + persistence to {@link DemandEmbeddingService}.
     */
    private void handleDemandApprovedEmbedding(BaseEvent<Map<String, Object>> event) {
        try {
            DemandPayload payload = objectMapper.convertValue(event.getPayload(), DemandPayload.class);

            if (payload == null || payload.getDemandId() == null) {
                log.warn("[DEMAND-EMBED] Invalid or missing demandId in DEMAND_APPROVED payload | eventId={}",
                        event.getEventId());
                return;
            }

            log.info("[DEMAND-EMBED] Processing DEMAND_APPROVED | demandId={} | eventId={}",
                    payload.getDemandId(), event.getEventId());

            demandEmbeddingService.embedAndStore(payload);

        } catch (Exception ex) {
            // Log but do not re-throw — embedding failure must not cause Kafka offset rollback
            log.error("[DEMAND-EMBED] Failed to process DEMAND_APPROVED embedding | eventId={} | error={}",
                    event.getEventId(), ex.getMessage(), ex);
        }
    }
}
