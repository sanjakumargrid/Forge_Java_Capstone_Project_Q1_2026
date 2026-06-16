package com.talentgrid.interview.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talentgrid.clients.notification.NotificationEventPublisher;
import com.talentgrid.kafka.consumer.BaseKafkaConsumer;
import com.talentgrid.kafka.events.base.BaseEvent;
import com.talentgrid.kafka.events.interview.InterviewPayload;
import com.talentgrid.kafka.topics.TalentGridTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class InterviewEventTranslator extends BaseKafkaConsumer<InterviewPayload> {

    private final NotificationEventPublisher notificationEventPublisher;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    @KafkaListener(
            topics = TalentGridTopics.INTERVIEW_EVENTS,
            groupId = "${spring.kafka.consumer.group-id:interview-service-group}",
            concurrency = "3",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onMessage(
            @Payload Object rawEvent,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset
    ) {

        log.info(
                "[INTERVIEW-TRANSLATOR] ▶ Message received | topic={} | partition={} | offset={}",
                topic,
                partition,
                offset
        );

        try {
            BaseEvent<InterviewPayload> event = extractPayload(rawEvent);

            if (event == null || event.getPayload() == null) {
                log.warn(
                        "[INTERVIEW-TRANSLATOR] Null event or payload at offset={} — skipping",
                        offset
                );
                return;
            }

            process(event);

        } catch (Exception e) {
            log.error(
                    "[INTERVIEW-TRANSLATOR] ✗ Failed to process message at offset={} | error={}",
                    offset,
                    e.getMessage(),
                    e
            );
        }
    }

    @Override
    protected void handleEvent(BaseEvent<InterviewPayload> event) {

        InterviewPayload interview = event.getPayload();
        String eventType = event.getEventType();
        String correlationId = event.getCorrelationId();

        log.info(
                "[INTERVIEW-TRANSLATOR] Translating event | type={} | interviewId={}",
                eventType,
                interview.getInterviewId()
        );

        switch (eventType) {
            case "INTERVIEW_SCHEDULED" ->
                    translateInterviewScheduled(interview, correlationId);

            case "INTERVIEW_UPDATED" ->
                    translateInterviewUpdated(interview, correlationId);

            case "INTERVIEW_COMPLETED" ->
                    translateInterviewCompleted(interview, correlationId);

            case "INTERVIEW_CANCELLED" ->
                    translateInterviewCancelled(interview, correlationId);

            default ->
                    log.debug(
                            "[INTERVIEW-TRANSLATOR] No notification mapping for eventType='{}' — skipping",
                            eventType
                    );
        }
    }

    private void translateInterviewScheduled(
            InterviewPayload interview,
            String correlationId
    ) {

        String title = "Interview Scheduled";

        String message = String.format(
                "Your %s interview has been scheduled on %s. Meeting link: %s",
                safe(interview.getType()),
                scheduledAt(interview),
                safe(interview.getMeetingLink())
        );

        publishNotification(
                interview,
                "INTERVIEW_SCHEDULED",
                title,
                message,
                "HIGH",
                "interview-scheduled",
                correlationId
        );
    }

    private void translateInterviewUpdated(
            InterviewPayload interview,
            String correlationId
    ) {

        String title = "Interview Updated";

        String message = String.format(
                "Your interview details have been updated. New schedule: %s. Meeting link: %s",
                scheduledAt(interview),
                safe(interview.getMeetingLink())
        );

        publishNotification(
                interview,
                "INTERVIEW_UPDATED",
                title,
                message,
                "NORMAL",
                "interview-updated",
                correlationId
        );
    }

    private void translateInterviewCompleted(
            InterviewPayload interview,
            String correlationId
    ) {

        String title = "Interview Completed";

        String message =
                "Your interview has been marked as completed. Our team will update you about the next steps.";

        publishNotification(
                interview,
                "INTERVIEW_COMPLETED",
                title,
                message,
                "NORMAL",
                "interview-completed",
                correlationId
        );
    }

    private void translateInterviewCancelled(
            InterviewPayload interview,
            String correlationId
    ) {

        String title = "Interview Cancelled";

        String message = String.format(
                "Your interview has been cancelled. Reason: %s.",
                interview.getCancellationReason() != null
                        ? interview.getCancellationReason()
                        : "Not specified"
        );

        publishNotification(
                interview,
                "INTERVIEW_CANCELLED",
                title,
                message,
                "HIGH",
                "interview-cancelled",
                correlationId
        );
    }

    private void publishNotification(
            InterviewPayload interview,
            String notificationType,
            String title,
            String message,
            String priority,
            String templateCode,
            String correlationId
    ) {

        notificationEventPublisher.sendInAppAndEmail(
                interview.getApplicationId() != null
                        ? interview.getApplicationId().toString()
                        : null,
                null,
                notificationType,
                title,
                message,
                "interview-service",
                interview.getInterviewId() != null
                        ? interview.getInterviewId().toString()
                        : null,
                "INTERVIEW",
                priority,
                templateCode,
                Map.of(
                        "interviewId", value(interview.getInterviewId()),
                        "applicationId", value(interview.getApplicationId()),
                        "interviewerIds", interview.getInterviewerIds() != null
                                ? interview.getInterviewerIds().toString()
                                : "",
                        "interviewType", safe(interview.getType()),
                        "scheduledAt", scheduledAt(interview),
                        "meetingLink", safe(interview.getMeetingLink()),
                        "status", safe(interview.getStatus()),
                        "cancellationReason",
                        interview.getCancellationReason() != null
                                ? interview.getCancellationReason()
                                : ""
                ),
                correlationId
        );

        log.info(
                "[INTERVIEW-TRANSLATOR] ✓ NOTIFICATION_SEND published | interviewId={} | type={}",
                interview.getInterviewId(),
                notificationType
        );
    }

    private String scheduledAt(InterviewPayload interview) {

        if (interview.getScheduledAt() == null) {
            return "Not scheduled";
        }

        return interview.getScheduledAt().format(DATE_TIME_FORMATTER);
    }

    private String safe(String value) {

        if (value == null || value.isBlank()) {
            return "N/A";
        }

        return value;
    }

    private String value(Long value) {

        return value != null ? value.toString() : "";
    }

    @SuppressWarnings("unchecked")
    private BaseEvent<InterviewPayload> extractPayload(Object rawEvent) throws Exception {

        if (rawEvent instanceof BaseEvent<?> base) {

            if (base.getPayload() instanceof InterviewPayload) {
                return (BaseEvent<InterviewPayload>) base;
            }

            InterviewPayload payload =
                    objectMapper.convertValue(
                            base.getPayload(),
                            InterviewPayload.class
                    );

            return BaseEvent.<InterviewPayload>builder()
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

                var javaType =
                        objectMapper.getTypeFactory()
                                .constructParametricType(
                                        BaseEvent.class,
                                        InterviewPayload.class
                                );

                return objectMapper.readValue(json, javaType);
            }

            String json =
                    objectMapper.writeValueAsString(value);

            var javaType =
                    objectMapper.getTypeFactory()
                            .constructParametricType(
                                    BaseEvent.class,
                                    InterviewPayload.class
                            );

            return objectMapper.readValue(json, javaType);
        }

        if (rawEvent instanceof String json) {

            var javaType =
                    objectMapper.getTypeFactory()
                            .constructParametricType(
                                    BaseEvent.class,
                                    InterviewPayload.class
                            );

            return objectMapper.readValue(json, javaType);
        }

        String json =
                objectMapper.writeValueAsString(rawEvent);

        var javaType =
                objectMapper.getTypeFactory()
                        .constructParametricType(
                                BaseEvent.class,
                                InterviewPayload.class
                        );

        return objectMapper.readValue(json, javaType);
    }
}