package com.talentgrid.clients.notification;

import com.talentgrid.kafka.events.base.BaseEvent;
import com.talentgrid.kafka.events.notification.NotificationPayload;
import com.talentgrid.kafka.producer.KafkaProducerService;
import com.talentgrid.kafka.topics.TalentGridTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Reusable Kafka notification publisher for all TalentGrid services.
 *
 * <p>Any service that needs to send a notification imports
 * {@code talentgrid-clients} and autowires this component.
 * It handles event construction and topic routing — producers
 * only need to supply the payload details.</p>
 *
 * <h3>Usage example (from demand-service)</h3>
 * <pre>{@code
 * notificationEventPublisher.sendInAppAndEmail(
 *     recipientUserId    = "101",
 *     recipientEmail     = "rm@company.com",
 *     notificationType   = "DEMAND_APPROVED",
 *     title              = "Demand approved",
 *     message            = "Demand 'Senior Java Dev' has been approved.",
 *     moduleName         = "demand-service",
 *     referenceId        = "demand-456",
 *     referenceType      = "DEMAND",
 *     priority           = "HIGH",
 *     templateId         = "demand-approved",
 *     templateVariables  = Map.of("demandTitle", "Senior Java Dev"),
 *     correlationId      = requestId
 * );
 * }</pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventPublisher {

    private final KafkaProducerService kafkaProducerService;

    /**
     * Sends an in-app + email notification.
     * The recipient email is passed through the payload but never logged.
     */
    public void sendInAppAndEmail(
            String recipientUserId,
            String recipientEmail,
            String notificationType,
            String title,
            String message,
            String moduleName,
            String referenceId,
            String referenceType,
            String priority,
            String templateId,
            Map<String, String> templateVariables,
            String correlationId) {

        publish(NotificationPayload.builder()
                .recipientUserId(recipientUserId)
                .recipientEmail(recipientEmail)
                .notificationType(notificationType)
                .title(title)
                .message(message)
                .channels(List.of("IN_APP", "EMAIL"))
                .moduleName(moduleName)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .priority(priority != null ? priority : "NORMAL")
                .templateId(templateId)
                .templateVariables(templateVariables)
                .build(),
                correlationId);
    }

    /**
     * Sends an in-app-only notification (no email).
     */
    public void sendInApp(
            String recipientUserId,
            String notificationType,
            String title,
            String message,
            String moduleName,
            String referenceId,
            String referenceType,
            String correlationId) {

        publish(NotificationPayload.builder()
                .recipientUserId(recipientUserId)
                .notificationType(notificationType)
                .title(title)
                .message(message)
                .channels(List.of("IN_APP"))
                .moduleName(moduleName)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .build(),
                correlationId);
    }

    /**
     * Low-level publish — accepts a fully-constructed payload.
     */
    public void publish(NotificationPayload payload, String correlationId) {

        BaseEvent<NotificationPayload> event = BaseEvent.<NotificationPayload>builder()
                .eventType("NOTIFICATION_SEND")
                .source(payload.getModuleName() != null ? payload.getModuleName() : "talentgrid")
                .correlationId(correlationId != null ? correlationId : UUID.randomUUID().toString())
                .payload(payload)
                .build();
        String partitionKey = payload.getRecipientUserId() != null
                ? payload.getRecipientUserId()
                : "unkeyed";
        kafkaProducerService.sendEvent(TalentGridTopics.NOTIFICATION_SEND, partitionKey, event);
    }
}
