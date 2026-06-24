package com.talentgrid.application.kafka.producer;

import com.talentgrid.application.application.entity.Application;
import com.talentgrid.kafka.events.application.ApplicationPayload;
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

@Component
@RequiredArgsConstructor
@Slf4j
public class ApplicationEventProducer {

    private static final String SOURCE = "application-service";

    private final KafkaProducerService kafkaProducerService;

    public void publishApplied(Application application) {
        ApplicationPayload payload = buildBasePayload(application);

        send(
                TalentGridTopics.APPLICATION_EVENTS,
                "APPLICATION_APPLIED",
                application.getId(),
                payload
        );
    }

    public void publishScreening(Application application) {
        ApplicationPayload payload = buildBasePayload(application);

        send(
                TalentGridTopics.APPLICATION_EVENTS,
                "APPLICATION_SCREENING",
                application.getId(),
                payload
        );
    }

    public void publishTechnical(Application application) {
        ApplicationPayload payload = buildBasePayload(application);

        send(
                TalentGridTopics.APPLICATION_EVENTS,
                "APPLICATION_TECHNICAL",
                application.getId(),
                payload
        );
    }

    public void publishInterview(Application application) {
        ApplicationPayload payload = buildBasePayload(application);

        send(
                TalentGridTopics.APPLICATION_EVENTS,
                "APPLICATION_INTERVIEW",
                application.getId(),
                payload
        );
    }

    public void publishFinalRound(Application application) {
        ApplicationPayload payload = buildBasePayload(application);

        send(
                TalentGridTopics.APPLICATION_EVENTS,
                "APPLICATION_FINAL_ROUND",
                application.getId(),
                payload
        );
    }

    public void publishOffered(Application application) {
        ApplicationPayload payload = buildBasePayload(application);

        send(
                TalentGridTopics.APPLICATION_EVENTS,
                "APPLICATION_OFFERED",
                application.getId(),
                payload
        );
    }

    public void publishHired(Application application) {
        ApplicationPayload payload = buildBasePayload(application);

        send(
                TalentGridTopics.APPLICATION_EVENTS,
                "APPLICATION_HIRED",
                application.getId(),
                payload
        );
    }

    public void publishRejected(Application application) {
        ApplicationPayload payload = buildBasePayload(application);

        send(
                TalentGridTopics.APPLICATION_EVENTS,
                "APPLICATION_REJECTED",
                application.getId(),
                payload
        );
    }

    public void publishJobPostingClosedNotification(Long jobPostingId, String jobTitle, String recruiterEmail) {
        if (recruiterEmail == null || recruiterEmail.isBlank()) {
            log.warn("Cannot send job posting closed notification: recruiterEmail is empty for jobPostingId={}", jobPostingId);
            return;
        }

        NotificationPayload payload = NotificationPayload.builder()
                .recipientEmail(recruiterEmail)
                .notificationType("JOB_POSTING_FILLED")
                .title("Position Filled: " + jobTitle + " (ID: " + jobPostingId + ")")
                .message("A candidate has been hired for the position: " + jobTitle + " (Job ID: " + jobPostingId + "). All other candidates have been auto-rejected. Please close this job posting in the system.")
                .channels(List.of("EMAIL", "IN_APP"))
                .moduleName("application-service")
                .referenceId(String.valueOf(jobPostingId))
                .referenceType("JOB_POSTING")
                .priority("HIGH")
                .build();

        String correlationId = UUID.randomUUID().toString();
        BaseEvent<NotificationPayload> event = BaseEvent.<NotificationPayload>builder()
                .eventType("JOB_POSTING_FILLED")
                .source(SOURCE)
                .correlationId(correlationId)
                .payload(payload)
                .build();

        kafkaProducerService.sendEvent(
                TalentGridTopics.NOTIFICATION_SEND,
                jobPostingId.toString(),
                event
        );

        log.info("Published JOB_POSTING_FILLED notification to recruiterEmail={} for jobPostingId={}", recruiterEmail, jobPostingId);
    }

    private ApplicationPayload buildBasePayload(Application application) {

        return ApplicationPayload.builder()
                .applicationId(application.getId())
                .candidateId(application.getCandidateId())

                // Application service now uses Job Posting ID.
                // Keeping demandId here only because shared Kafka ApplicationPayload
                // still has demandId field.
                .demandId(application.getJobPostingId())

                .source(
                        application.getSource() != null
                                ? application.getSource().name()
                                : null
                )
                .currentStage(
                        application.getCurrentStage() != null
                                ? application.getCurrentStage().name()
                                : null
                )
                .aiScore(application.getAiScore())
                .stageMoveReason(application.getStageMoveReason())
                .rejectionReason(application.getRejectionReason())
                .blockedFromReapply(application.getBlockedFromReapply())
                .build();
    }

    private void send(
            String topic,
            String eventType,
            Long applicationId,
            ApplicationPayload payload
    ) {
        String correlationId = UUID.randomUUID().toString();

        String key =
                applicationId != null
                        ? applicationId.toString()
                        : correlationId;

        BaseEvent<ApplicationPayload> event =
                BaseEvent.<ApplicationPayload>builder()
                        .eventType(eventType)
                        .source(SOURCE)
                        .correlationId(correlationId)
                        .payload(payload)
                        .build();

        kafkaProducerService.sendEvent(
                topic,
                key,
                event
        );

        log.info(
                "Published {} event to topic={} for applicationId={}",
                eventType,
                topic,
                applicationId
        );
    }
}