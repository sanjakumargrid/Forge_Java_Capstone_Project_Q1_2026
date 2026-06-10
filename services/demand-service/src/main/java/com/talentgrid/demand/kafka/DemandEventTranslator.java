package com.talentgrid.demand.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talentgrid.clients.notification.NotificationEventPublisher;
import com.talentgrid.kafka.consumer.BaseKafkaConsumer;
import com.talentgrid.kafka.events.base.BaseEvent;
import com.talentgrid.kafka.events.demand.DemandPayload;
import com.talentgrid.kafka.topics.TalentGridTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Event translator: consumes DEMAND_CREATED / DEMAND_APPROVED events from
 * {@code demand-events} and publishes {@code NOTIFICATION_SEND} events to
 * {@code notification.send}.
 *
 * <p><strong>FIX:</strong> Changed initial receipt log from DEBUG to INFO and
 * added explicit recipientEmail-presence check at INFO level before calling
 * NotificationEventPublisher. Previously, a null recipientEmail would silently
 * pass through to NotificationEventPublisher, reach EmailNotificationChannel,
 * log a WARN, and skip — with no indication in DemandEventTranslator logs
 * that the field was missing at the source.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DemandEventTranslator extends BaseKafkaConsumer<DemandPayload> {

    private final NotificationEventPublisher notificationEventPublisher;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = TalentGridTopics.DEMAND_EVENTS,
            groupId = "${spring.kafka.consumer.group-id:demand-service-group}",
            concurrency = "3",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onMessage(
            @Payload Object rawEvent,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        // FIX: INFO level (was DEBUG) — confirms translator is actually receiving messages.
        // Without this, a missing log here vs. a debug-suppressed log were indistinguishable.
        log.info("[DEMAND-TRANSLATOR] ▶ Message received | topic={} | partition={} | offset={}",
                topic, partition, offset);

        try {
            BaseEvent<DemandPayload> event = extractPayload(rawEvent);

            if (event == null || event.getPayload() == null) {
                log.warn("[DEMAND-TRANSLATOR] Null event or payload at offset={} — skipping", offset);
                return;
            }

            process(event);

        } catch (Exception e) {
            log.error("[DEMAND-TRANSLATOR] ✗ Failed to process message at offset={} | error={}",
                    offset, e.getMessage(), e);
            // Do not rethrow — prevents consumer from sticking on a poison-pill message.
            // TODO: Route to DLT (Dead Letter Topic) when talentgrid-kafka DLT support is added.
        }
    }

    @Override
    protected void handleEvent(BaseEvent<DemandPayload> event) {
        DemandPayload demand = event.getPayload();
        String eventType = event.getEventType();
        String correlationId = event.getCorrelationId();

        log.info("[DEMAND-TRANSLATOR] Translating event | type={} | demandId={} | raisedBy={}",
                eventType, demand.getDemandId(), demand.getRaisedBy());

        switch (eventType) {
            case "DEMAND_CREATED" -> translateDemandCreated(demand, correlationId);
            case "DEMAND_APPROVED" -> translateDemandApproved(demand, correlationId);
            default -> log.debug("[DEMAND-TRANSLATOR] No notification mapping for eventType='{}' — skipping", eventType);
        }
    }

    // ─────────────────────────────────────────────────────────
    // Translation Methods — one per business event type
    // ─────────────────────────────────────────────────────────

    private void translateDemandCreated(DemandPayload demand, String correlationId) {

        // FIX: Validate recipientEmail HERE and log clearly so the problem is caught
        // at the source (DemandEventTranslator) rather than silently in EmailNotificationChannel.
        if (demand.getRecipientEmail() == null || demand.getRecipientEmail().isBlank()) {
            log.warn("[DEMAND-TRANSLATOR] ⚠ recipientEmail is null/blank for demandId={}. " +
                    "Email notification will be skipped. Ensure the POST /api/demands request " +
                    "body includes a non-empty 'recipientEmail' field.", demand.getDemandId());
        } else {
            log.info("[DEMAND-TRANSLATOR] ▶ recipientEmail is present for demandId={} — email will be sent",
                    demand.getDemandId());
        }

        String title = "New Demand Created: " + demand.getTitle();
        String message = String.format(
                "A new demand for '%s' (%s, %s) has been raised by %s. Skills required: %s.",
                demand.getTitle(),
                demand.getLevel() != null ? demand.getLevel() : "N/A",
                demand.getLocation() != null ? demand.getLocation() : "Remote",
                demand.getRaisedBy(),
                demand.getSkills() != null ? String.join(", ", demand.getSkills()) : "Not specified"
        );

        notificationEventPublisher.sendInAppAndEmail(
                demand.getRaisedBy(),
                demand.getRecipientEmail(),
                "DEMAND_CREATED",
                title,
                message,
                "demand-service",
                demand.getDemandId(),
                "DEMAND",
                "NORMAL",
                "demand-created",
                Map.of(
                        "demandTitle",    demand.getTitle()    != null ? demand.getTitle()    : "",
                        "demandLevel",    demand.getLevel()    != null ? demand.getLevel()    : "N/A",
                        "demandLocation", demand.getLocation() != null ? demand.getLocation() : "Remote",
                        "raisedBy",       demand.getRaisedBy() != null ? demand.getRaisedBy() : "Unknown",
                        "skills",         demand.getSkills()   != null ? String.join(", ", demand.getSkills()) : "N/A"
                ),
                correlationId
        );

        log.info("[DEMAND-TRANSLATOR] ✓ NOTIFICATION_SEND published | demandId={} | type=DEMAND_CREATED",
                demand.getDemandId());
    }

    private void translateDemandApproved(DemandPayload demand, String correlationId) {

        if (demand.getRecipientEmail() == null || demand.getRecipientEmail().isBlank()) {
            log.warn("[DEMAND-TRANSLATOR] ⚠ recipientEmail is null/blank for DEMAND_APPROVED | demandId={}",
                    demand.getDemandId());
        }

        String title = "Demand Approved: " + demand.getTitle();
        String message = String.format(
                "Your demand for '%s' has been approved. Talent acquisition team will begin sourcing candidates shortly.",
                demand.getTitle()
        );

        notificationEventPublisher.sendInAppAndEmail(
                demand.getRaisedBy(),
                demand.getRecipientEmail(),
                "DEMAND_APPROVED",
                title,
                message,
                "demand-service",
                demand.getDemandId(),
                "DEMAND",
                "HIGH",
                "demand-approved",
                Map.of(
                        "demandTitle", demand.getTitle()    != null ? demand.getTitle()    : "",
                        "raisedBy",    demand.getRaisedBy() != null ? demand.getRaisedBy() : "Unknown"
                ),
                correlationId
        );
        log.info("[DEMAND-TRANSLATOR] ✓ NOTIFICATION_SEND published | demandId={} | type=DEMAND_APPROVED",
                demand.getDemandId());
    }

    // ─────────────────────────────────────────────────────────
    // Deserialization Utility
    // ─────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private BaseEvent<DemandPayload> extractPayload(Object rawEvent) throws Exception {

        if (rawEvent instanceof BaseEvent<?> base) {
            if (base.getPayload() instanceof DemandPayload) {
                return (BaseEvent<DemandPayload>) base;
            }
            DemandPayload payload =
                    objectMapper.convertValue(base.getPayload(), DemandPayload.class);
            return BaseEvent.<DemandPayload>builder()
                    .eventId(base.getEventId())
                    .eventType(base.getEventType())
                    .timestamp(base.getTimestamp())
                    .source(base.getSource())
                    .version(base.getVersion())
                    .correlationId(base.getCorrelationId())
                    .payload(payload)
                    .build();
        }

        if (rawEvent instanceof org.apache.kafka.clients.consumer.ConsumerRecord<?, ?> record) {
            Object value = record.value();
            if (value instanceof String json) {
                var javaType = objectMapper.getTypeFactory()
                        .constructParametricType(BaseEvent.class, DemandPayload.class);
                return objectMapper.readValue(json, javaType);
            }
            String json = objectMapper.writeValueAsString(value);
            var javaType = objectMapper.getTypeFactory()
                    .constructParametricType(BaseEvent.class, DemandPayload.class);
            return objectMapper.readValue(json, javaType);
        }

        if (rawEvent instanceof String json) {
            var javaType = objectMapper.getTypeFactory()
                    .constructParametricType(BaseEvent.class, DemandPayload.class);
            return objectMapper.readValue(json, javaType);
        }

        String json = objectMapper.writeValueAsString(rawEvent);
        var javaType = objectMapper.getTypeFactory()
                .constructParametricType(BaseEvent.class, DemandPayload.class);
        return objectMapper.readValue(json, javaType);
    }
}