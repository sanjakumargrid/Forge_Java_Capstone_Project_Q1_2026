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


@Component
@RequiredArgsConstructor
@Slf4j
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
            // Do not rethrow — prevents consumer from getting stuck on a poison-pill message.
            // TODO: Route to demand-events.dlq (Dead Letter Queue) when DLT support is added.
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
            case "DEMAND_CREATED"         -> translateDemandCreated(demand, correlationId);
            case "DEMAND_SUBMITTED"       -> translateDemandSubmitted(demand, correlationId);
            case "DEMAND_APPROVED"        -> translateDemandApproved(demand, correlationId);
            case "DEMAND_EXTERNAL_OPENED" -> translateDemandExternalOpened(demand, correlationId);
            case "DEMAND_CLOSED"          -> translateDemandClosed(demand, correlationId);
            default -> log.debug(
                    "[DEMAND-TRANSLATOR] No notification mapping for eventType='{}' — skipping", eventType);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Translation Methods — one per notifiable business event type
    // ─────────────────────────────────────────────────────────────────────────────

    private void translateDemandCreated(DemandPayload demand, String correlationId) {
        warnIfRecipientEmailMissing(demand, "DEMAND_CREATED");
        warnIfRecipientSlackIdMissing(demand, "DEMAND_CREATED");


        String title = "New Demand Created: " + demand.getTitle();
        String message = String.format(
                "A new demand for '%s' (%s, %s) has been raised by %s. Skills required: %s.",
                demand.getTitle(),
                demand.getLevel() != null ? demand.getLevel() : "N/A",
                demand.getLocation() != null ? demand.getLocation() : "Remote",
                demand.getRaisedBy() != null ? demand.getRaisedBy() : "Unknown",
                demand.getSkills() != null ? String.join(", ", demand.getSkills()) : "Not specified"
        );

        notificationEventPublisher.sendInAppAndEmail(
                demand.getCreatedBy() != null ? demand.getCreatedBy().toString() : demand.getRaisedBy(),
                demand.getRecipientEmail(),
                demand.getRecipientSlackId(),
                "DEMAND_CREATED",
                title,
                message,
                "demand-service",
                demand.getDemandId() != null ? demand.getDemandId().toString() : null,
                "DEMAND",
                "NORMAL",
                "demand-created",
                Map.of(
                        "demandTitle",    safe(demand.getTitle()),
                        "demandLevel",    safe(demand.getLevel(), "N/A"),
                        "demandLocation", safe(demand.getLocation(), "Remote"),
                        "raisedBy",       safe(demand.getRaisedBy(), "Unknown"),
                        "skills",         demand.getSkills() != null
                                ? String.join(", ", demand.getSkills()) : "N/A"
                ),
                correlationId
        );

        log.info("[DEMAND-TRANSLATOR] ✓ NOTIFICATION_SEND published | demandId={} | type=DEMAND_CREATED",
                demand.getDemandId());
    }

    private void translateDemandSubmitted(DemandPayload demand, String correlationId) {
        warnIfRecipientEmailMissing(demand, "DEMAND_SUBMITTED");
        warnIfRecipientSlackIdMissing(demand, "DEMAND_SUBMITTED");


        String title = "Demand Submitted for Approval: " + demand.getTitle();
        String message = String.format(
                "Your demand for '%s' (%s, %s) has been submitted for approval. Skills required: %s.",
                demand.getTitle(),
                demand.getLevel() != null ? demand.getLevel() : "N/A",
                demand.getLocation() != null ? demand.getLocation() : "Remote",
                demand.getSkills() != null ? String.join(", ", demand.getSkills()) : "Not specified"
        );

        notificationEventPublisher.sendInAppAndEmail(
                demand.getCreatedBy() != null ? demand.getCreatedBy().toString() : demand.getRaisedBy(),
                demand.getRecipientEmail(),
                demand.getRecipientSlackId(),
                "DEMAND_SUBMITTED",
                title,
                message,
                "demand-service",
                demand.getDemandId() != null ? demand.getDemandId().toString() : null,
                "DEMAND",
                "NORMAL",
                "demand-created",   // reuses the same HTML template as DEMAND_CREATED
                Map.of(
                        "demandTitle",    safe(demand.getTitle()),
                        "demandLevel",    safe(demand.getLevel(), "N/A"),
                        "demandLocation", safe(demand.getLocation(), "Remote"),
                        "raisedBy",       safe(demand.getRaisedBy(), "Unknown"),
                        "skills",         demand.getSkills() != null
                                ? String.join(", ", demand.getSkills()) : "N/A"
                ),
                correlationId
        );

        log.info("[DEMAND-TRANSLATOR] ✓ NOTIFICATION_SEND published | demandId={} | type=DEMAND_SUBMITTED",
                demand.getDemandId());
    }

    private void translateDemandApproved(DemandPayload demand, String correlationId) {
        warnIfRecipientEmailMissing(demand, "DEMAND_APPROVED");
        warnIfRecipientSlackIdMissing(demand, "DEMAND_APPROVED");


        String title = "Demand Approved: " + demand.getTitle();
        String message = String.format(
                "Your demand for '%s' has been approved by %s. " +
                        "The talent acquisition team will begin sourcing candidates shortly.",
                demand.getTitle(),
                demand.getApproverName() != null ? demand.getApproverName() : "the approver"
        );

        notificationEventPublisher.sendInAppAndEmail(
                demand.getCreatedBy() != null ? demand.getCreatedBy().toString() : demand.getRaisedBy(),
                demand.getRecipientEmail(),
                demand.getRecipientSlackId(),
                "DEMAND_APPROVED",
                title,
                message,
                "demand-service",
                demand.getDemandId() != null ? demand.getDemandId().toString() : null,
                "DEMAND",
                "HIGH",
                "demand-approved",
                Map.of(
                        "demandTitle",   safe(demand.getTitle()),
                        "raisedBy",      safe(demand.getRaisedBy(), "Unknown"),
                        "approverName",  safe(demand.getApproverName(), "Your Manager")
                ),
                correlationId
        );

        log.info("[DEMAND-TRANSLATOR] ✓ NOTIFICATION_SEND published | demandId={} | type=DEMAND_APPROVED",
                demand.getDemandId());
    }

    private void translateDemandExternalOpened(DemandPayload demand, String correlationId) {
        warnIfRecipientEmailMissing(demand, "DEMAND_EXTERNAL_OPENED");
        warnIfRecipientSlackIdMissing(demand, "DEMAND_EXTERNAL_OPENED");


        String recruiterInfo = demand.getAssignedRecruiterName() != null
                ? demand.getAssignedRecruiterName()
                : "an external recruiter";

        String title = "External Hiring Opened: " + demand.getTitle();
        String message = String.format(
                "Your demand for '%s' has been opened for external hiring. " +
                        "%s has been assigned to source candidates externally. " +
                        "Positions open: %d (internal filled: %d).",
                demand.getTitle(),
                recruiterInfo,
                demand.getRequiredCount() != null ? demand.getRequiredCount() : 0,
                demand.getInternalFilledCount() != null ? demand.getInternalFilledCount() : 0
        );

        notificationEventPublisher.sendInAppAndEmail(
                demand.getCreatedBy() != null ? demand.getCreatedBy().toString() : demand.getRaisedBy(),
                demand.getRecipientEmail(),
                demand.getRecipientSlackId(),
                "DEMAND_EXTERNAL_OPENED",
                title,
                message,
                "demand-service",
                demand.getDemandId() != null ? demand.getDemandId().toString() : null,
                "DEMAND",
                "NORMAL",
                "demand-external-opened",
                Map.of(
                        "demandTitle",          safe(demand.getTitle()),
                        "raisedBy",             safe(demand.getRaisedBy(), "Unknown"),
                        "recruiterName",         safe(demand.getAssignedRecruiterName(), "External Recruiter"),
                        "requiredCount",         demand.getRequiredCount() != null
                                ? demand.getRequiredCount().toString() : "0",
                        "internalFilledCount",   demand.getInternalFilledCount() != null
                                ? demand.getInternalFilledCount().toString() : "0",
                        "location",              safe(demand.getLocation(), "Remote"),
                        "skills",                demand.getSkills() != null
                                ? String.join(", ", demand.getSkills()) : "N/A"
                ),
                correlationId
        );

        log.info("[DEMAND-TRANSLATOR] ✓ NOTIFICATION_SEND published | demandId={} | type=DEMAND_EXTERNAL_OPENED",
                demand.getDemandId());
    }

    private void translateDemandClosed(DemandPayload demand, String correlationId) {
        warnIfRecipientEmailMissing(demand, "DEMAND_CLOSED");
        warnIfRecipientSlackIdMissing(demand, "DEMAND_CLOSED");

        int required  = demand.getRequiredCount()        != null ? demand.getRequiredCount()        : 0;
        int internal  = demand.getInternalFilledCount()  != null ? demand.getInternalFilledCount()  : 0;
        int external  = demand.getExternalFilledCount()  != null ? demand.getExternalFilledCount()  : 0;
        int total     = demand.getRecruitedCount()       != null ? demand.getRecruitedCount()       : (internal + external);

        String closureReason = demand.getClosureReason() != null
                ? demand.getClosureReason().replace("_", " ")
                : "Not specified";

        String title = "Demand Closed: " + demand.getTitle();
        String message = String.format(
                "Your demand for '%s' has been closed. Reason: %s. " +
                        "Total filled: %d of %d (Internal: %d, External: %d).",
                demand.getTitle(),
                closureReason,
                total,
                required,
                internal,
                external
        );

        notificationEventPublisher.sendInAppAndEmail(
                demand.getCreatedBy() != null ? demand.getCreatedBy().toString() : demand.getRaisedBy(),
                demand.getRecipientEmail(),
                demand.getRecipientSlackId(),
                "DEMAND_CLOSED",
                title,
                message,
                "demand-service",
                demand.getDemandId() != null ? demand.getDemandId().toString() : null,
                "DEMAND",
                "NORMAL",
                "demand-closed",
                Map.of(
                        "demandTitle",    safe(demand.getTitle()),
                        "raisedBy",       safe(demand.getRaisedBy(), "Unknown"),
                        "closureReason",  closureReason,
                        "requiredCount",  String.valueOf(required),
                        "internalFilled", String.valueOf(internal),
                        "externalFilled", String.valueOf(external),
                        "totalFilled",    String.valueOf(total)
                ),
                correlationId
        );

        log.info("[DEMAND-TRANSLATOR] ✓ NOTIFICATION_SEND published | demandId={} | type=DEMAND_CLOSED",
                demand.getDemandId());
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Utilities
    // ─────────────────────────────────────────────────────────────────────────────

    /** Logs a WARNING if recipientEmail is missing — identifies the bug at the source. */
    private void warnIfRecipientEmailMissing(DemandPayload demand, String eventType) {
        if (demand.getRecipientEmail() == null || demand.getRecipientEmail().isBlank()) {
            log.warn("[DEMAND-TRANSLATOR] ⚠ recipientEmail is null/blank for {} | demandId={}. " +
                            "Email notification will be skipped by EmailNotificationChannel.",
                    eventType, demand.getDemandId());
        } else {
            log.info("[DEMAND-TRANSLATOR] ▶ recipientEmail present for {} | demandId={} — email will be sent",
                    eventType, demand.getDemandId());
        }
    }

    private void warnIfRecipientSlackIdMissing(DemandPayload demand, String eventType) {
        if (demand.getRecipientSlackId() == null || demand.getRecipientSlackId().isBlank()) {
            log.warn("[DEMAND-TRANSLATOR] ⚠ recipientSlackId is null/blank for {} | demandId={}. " +
                            "Slack notification will be skipped by SlackNotificationChannel.",
                    eventType, demand.getDemandId());
        } else {
            log.info("[DEMAND-TRANSLATOR] ▶ recipientSlackId present for {} | demandId={} — Message will be sent",
                    eventType, demand.getDemandId());
        }
    }

    /** Returns the value or empty string if null. */
    private String safe(String value) {
        return value != null ? value : "";
    }

    /** Returns the value or the fallback if null/blank. */
    private String safe(String value, String fallback) {
        return (value != null && !value.isBlank()) ? value : fallback;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Deserialization Utility
    // ─────────────────────────────────────────────────────────────────────────────

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