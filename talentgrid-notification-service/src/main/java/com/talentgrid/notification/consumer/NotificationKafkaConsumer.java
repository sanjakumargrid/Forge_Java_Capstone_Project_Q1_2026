package com.talentgrid.notification.consumer;

import com.talentgrid.kafka.consumer.BaseKafkaConsumer;
import com.talentgrid.kafka.events.base.BaseEvent;
import com.talentgrid.kafka.events.notification.NotificationPayload;
import com.talentgrid.kafka.topics.TalentGridTopics;
import com.talentgrid.notification.service.NotificationOrchestrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.apache.kafka.clients.consumer.ConsumerRecord;

/**
 * PLAT-05 Kafka consumer for the {@code notification.send} topic.
 *
 * <p>Consumer group: {@code notification-service-group}<br>
 * Topic: {@code notification.send}<br>
 * Concurrency: 3 (one thread per partition)</p>
 *
 * <p><strong>FIX:</strong> Changed initial receipt log from DEBUG to INFO so
 * you can confirm the consumer is receiving messages without needing to enable
 * DEBUG logging. Previously, the only evidence of consumer activity was at
 * DEBUG level, making it impossible to distinguish "consumer not receiving"
 * from "consumer receiving but failing silently" without changing log config.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationKafkaConsumer extends BaseKafkaConsumer<NotificationPayload> {

    private final NotificationOrchestrationService orchestrationService;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @KafkaListener(
            topics = TalentGridTopics.NOTIFICATION_SEND,
            groupId = "${spring.kafka.consumer.group-id:notification-service-group}",
            concurrency = "3",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onMessage(
            @Payload Object rawEvent,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        // FIX: INFO level (was DEBUG) so consumer receipt is always visible in logs.
        // This is the first log line you should see when a notification.send message arrives.
        log.info("[NOTIFICATION-CONSUMER] ▶ Message received | topic={} | partition={} | offset={}",
                topic, partition, offset);

        try {
            BaseEvent<NotificationPayload> event = extractPayload(rawEvent);

            if (event == null || event.getPayload() == null) {
                log.warn("[NOTIFICATION-CONSUMER] Null event or payload at offset={} — skipping", offset);
                return;
            }

            // FIX: Log payload summary at INFO level so you can see channels and email presence
            NotificationPayload p = event.getPayload();
            log.info("[NOTIFICATION-CONSUMER] ▶ Payload deserialized | type={} | userId={} | channels={} | emailPresent={} | slackIdPresent={}",
                    p.getNotificationType(),
                    p.getRecipientUserId(),
                    p.getChannels(),
                    p.getRecipientEmail() != null && !p.getRecipientEmail().isBlank(),
                    p.getRecipientSlackId() != null && !p.getRecipientSlackId().isBlank());

            process(event);

        } catch (Exception e) {
            log.error("[NOTIFICATION-CONSUMER] ✗ Failed to process message at offset={} | error={}",
                    offset, e.getMessage(), e);
            // Do not rethrow — prevents consumer from being stuck on a poison-pill message.
            // TODO: Route to dead-letter topic when talentgrid-kafka DLT support is added.
        }
    }

    @Override
    protected void handleEvent(BaseEvent<NotificationPayload> event) {
        orchestrationService.process(event.getPayload());
    }

    /**
     * Converts the raw Kafka payload (which may be a LinkedHashMap due to type erasure)
     * into a typed {@code BaseEvent<NotificationPayload>} using Jackson.
     */
    @SuppressWarnings("unchecked")
    private BaseEvent<NotificationPayload> extractPayload(Object rawEvent) throws Exception {
        if (rawEvent instanceof BaseEvent) {
            BaseEvent<?> base = (BaseEvent<?>) rawEvent;
            if (base.getPayload() instanceof NotificationPayload) {
                return (BaseEvent<NotificationPayload>) rawEvent;
            }
            // Convert LinkedHashMap payload to NotificationPayload (type erasure at runtime)
            NotificationPayload payload = objectMapper.convertValue(
                    base.getPayload(), NotificationPayload.class);
            return BaseEvent.<NotificationPayload>builder()
                    .eventId(base.getEventId())
                    .eventType(base.getEventType())
                    .timestamp(base.getTimestamp())
                    .source(base.getSource())
                    .version(base.getVersion())
                    .correlationId(base.getCorrelationId())
                    .payload(payload)
                    .build();
        }
        if (rawEvent instanceof ConsumerRecord<?, ?> record) {
            rawEvent = record.value();
        }
        String json = objectMapper.writeValueAsString(rawEvent);
        var javaType = objectMapper.getTypeFactory()
                .constructParametricType(BaseEvent.class, NotificationPayload.class);
        return objectMapper.readValue(json, javaType);
    }
}