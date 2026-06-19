package com.talentgrid.application.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talentgrid.clients.notification.NotificationEventPublisher;
import com.talentgrid.kafka.consumer.BaseKafkaConsumer;
import com.talentgrid.kafka.events.application.ApplicationPayload;
import com.talentgrid.kafka.events.base.BaseEvent;
import com.talentgrid.kafka.topics.TalentGridTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

import com.talentgrid.application.client.CandidateClient;
import com.talentgrid.application.application.dto.candidate.ExternalCandidateDto;
import com.talentgrid.application.application.repository.ApplicationRepository;
import com.talentgrid.application.application.entity.Application;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApplicationEventTranslator extends BaseKafkaConsumer<ApplicationPayload> {

  private final NotificationEventPublisher notificationEventPublisher;
  private final ObjectMapper objectMapper;
  private final CandidateClient candidateClient;
  private final ApplicationRepository applicationRepository;

  @KafkaListener(
          topics = TalentGridTopics.APPLICATION_EVENTS,
          groupId = "${spring.kafka.consumer.group-id:application-service-group}",
          concurrency = "3",
          containerFactory = "kafkaListenerContainerFactory"
  )
  public void onMessage(
          @Payload Object rawEvent,
          @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
          @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
          @Header(KafkaHeaders.OFFSET) long offset) {

    log.info("[APPLICATION-TRANSLATOR] ▶ Message received | topic={} | partition={} | offset={}",
            topic, partition, offset);

    try {
      BaseEvent<ApplicationPayload> event = extractPayload(rawEvent);

      if (event == null || event.getPayload() == null) {
        log.warn("[APPLICATION-TRANSLATOR] Null event or payload at offset={} — skipping", offset);
        return;
      }

      process(event);

    } catch (Exception e) {
      log.error("[APPLICATION-TRANSLATOR] ✗ Failed to process message at offset={} | error={}",
              offset, e.getMessage(), e);
    }
  }

  @Override
  protected void handleEvent(BaseEvent<ApplicationPayload> event) {
    ApplicationPayload application = event.getPayload();
    String eventType = event.getEventType();
    String correlationId = event.getCorrelationId();

    log.info("[APPLICATION-TRANSLATOR] Translating event | type={} | applicationId={} | candidateId={}",
            eventType, application.getApplicationId(), application.getCandidateId());

    switch (eventType) {
      case "APPLICATION_APPLIED"     -> translateStageMove(application, correlationId, "APPLICATION_APPLIED",
              "Application Received",
              "Your application has been submitted successfully. Our team will review it shortly.",
              "application-applied", "NORMAL");

      case "APPLICATION_SCREENING"   -> translateStageMove(application, correlationId, "APPLICATION_SCREENING",
              "Moving to Screening",
              "Congratulations! Your application has moved to the Screening stage.",
              "application-screening", "NORMAL");

      case "APPLICATION_TECHNICAL"   -> translateStageMove(application, correlationId, "APPLICATION_TECHNICAL",
              "Moving to Technical Round",
              "You have advanced to the Technical assessment stage. Prepare well!",
              "application-technical", "NORMAL");

      case "APPLICATION_INTERVIEW"   -> translateStageMove(application, correlationId, "APPLICATION_INTERVIEW",
              "Interview Scheduled",
              "Your application is progressing to the Interview stage. Check your email for scheduling details.",
              "application-interview", "HIGH");

      case "APPLICATION_FINAL_ROUND" -> translateStageMove(application, correlationId, "APPLICATION_FINAL_ROUND",
              "Final Round",
              "Excellent progress! You have reached the Final Round of the selection process.",
              "application-final-round", "HIGH");

      case "APPLICATION_OFFERED"     -> translateStageMove(application, correlationId, "APPLICATION_OFFERED",
              "Offer Extended",
              "Congratulations! An offer has been extended for your application. Please check your email for details.",
              "application-offered", "HIGH");

      case "APPLICATION_HIRED"       -> translateStageMove(application, correlationId, "APPLICATION_HIRED",
              "Welcome Aboard!",
              "Congratulations! You have been hired. Our HR team will reach out with onboarding details.",
              "application-hired", "HIGH");

      case "APPLICATION_REJECTED"    -> translateRejected(application, correlationId);

      default -> log.debug("[APPLICATION-TRANSLATOR] No notification mapping for eventType='{}' — skipping", eventType);
    }
  }

  // ─────────────────────────────────────────────────────────
  // Translation Methods
  // ─────────────────────────────────────────────────────────

  private void translateStageMove(
          ApplicationPayload application,
          String correlationId,
          String notificationType,
          String title,
          String message,
          String templateId,
          String priority) {

    // Notifications go to the candidate — use candidateId as the recipientUserId.
    // recipientEmail is not carried in ApplicationPayload; the notification-service
    // delivers in-app; email will only work if the candidate's email is looked up
    // separately. For now, we send IN_APP only via sendInApp.
    notificationEventPublisher.sendInApp(
            application.getCandidateId() != null ? application.getCandidateId().toString() : null,
            notificationType,
            title,
            message,
            "application-service",
            application.getApplicationId() != null ? application.getApplicationId().toString() : null,
            "APPLICATION",
            correlationId
    );

    log.info("[APPLICATION-TRANSLATOR] ✓ NOTIFICATION_SEND published | applicationId={} | type={}",
            application.getApplicationId(), notificationType);
  }

  private void translateRejected(ApplicationPayload application, String correlationId) {
    String reason = application.getRejectionReason() != null
            ? application.getRejectionReason()
            : "Not specified";

    String candidateEmail = null;
    String candidateName = "Candidate";
    try {
      if (application.getCandidateId() != null) {
        ExternalCandidateDto candidate = candidateClient.getCandidateById(application.getCandidateId());
        if (candidate != null) {
          candidateEmail = candidate.getEmail();
          candidateName = candidate.getFirstName() + " " + candidate.getLastName();
        }
      }
    } catch (Exception e) {
      log.warn("[APPLICATION-TRANSLATOR] Failed to fetch candidate email for candidateId={} - sending in-app only", application.getCandidateId(), e);
    }

    String rejectedRound = "the recruitment process";
    try {
      if (application.getApplicationId() != null) {
        Application app = applicationRepository.findById(application.getApplicationId()).orElse(null);
        if (app != null) {
            if (app.getFinalRoundAt() != null) rejectedRound = "Final Round";
            else if (app.getInterviewAt() != null) rejectedRound = "Interview Round";
            else if (app.getTechnicalAt() != null) rejectedRound = "Technical Round";
            else if (app.getScreeningAt() != null) rejectedRound = "Screening Round";
            else rejectedRound = "Initial Resume Screening";
        }
      }
    } catch (Exception e) {
        log.warn("[APPLICATION-TRANSLATOR] Failed to fetch application for determining round", e);
    }

    String title = "Update on Your Application at Grid Dynamics";
    String message = String.format(
            "Dear %s,\n\n" +
            "Thank you for taking the time to consider Grid Dynamics and for interviewing with our team. " +
            "We appreciate the opportunity to learn about your background and experience.\n\n" +
            "After careful consideration, we regret to inform you that we will not be moving forward with your application " +
            "following the %s. Reason: %s.\n\n" +
            "We were impressed by your skills and encourage you to apply for other suitable roles at Grid Dynamics in the future. " +
            "We wish you the best of luck in your career endeavors.\n\n" +
            "Best regards,\n" +
            "The Grid Dynamics Recruiting Team",
            candidateName,
            rejectedRound,
            reason
    );

    if (candidateEmail != null) {
      notificationEventPublisher.sendInAppAndEmail(
              application.getCandidateId().toString(),
              candidateEmail,
              "APPLICATION_REJECTED",
              title,
              message,
              "application-service",
              application.getApplicationId() != null ? application.getApplicationId().toString() : null,
              "APPLICATION",
              "NORMAL",
              "application-rejected",
              Map.of(
                      "applicationId", String.valueOf(application.getApplicationId()),
                      "reason", reason,
                      "candidateName", candidateName,
                      "companyName", "Grid Dynamics",
                      "rejectedRound", rejectedRound
              ),
              correlationId
      );
    } else {
      notificationEventPublisher.sendInApp(
              application.getCandidateId() != null ? application.getCandidateId().toString() : null,
              "APPLICATION_REJECTED",
              title,
              message,
              "application-service",
              application.getApplicationId() != null ? application.getApplicationId().toString() : null,
              "APPLICATION",
              correlationId
      );
    }

    log.info("[APPLICATION-TRANSLATOR] ✓ NOTIFICATION_SEND published | applicationId={} | type=APPLICATION_REJECTED",
            application.getApplicationId());
  }

  // ─────────────────────────────────────────────────────────
  // Deserialization Utility
  // ─────────────────────────────────────────────────────────

  @SuppressWarnings("unchecked")
  private BaseEvent<ApplicationPayload> extractPayload(Object rawEvent) throws Exception {

    if (rawEvent instanceof BaseEvent<?> base) {
      if (base.getPayload() instanceof ApplicationPayload) {
        return (BaseEvent<ApplicationPayload>) base;
      }
      ApplicationPayload payload = objectMapper.convertValue(base.getPayload(), ApplicationPayload.class);
      return BaseEvent.<ApplicationPayload>builder()
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
                .constructParametricType(BaseEvent.class, ApplicationPayload.class);
        return objectMapper.readValue(json, javaType);
      }
      String json = objectMapper.writeValueAsString(value);
      var javaType = objectMapper.getTypeFactory()
              .constructParametricType(BaseEvent.class, ApplicationPayload.class);
      return objectMapper.readValue(json, javaType);
    }

    if (rawEvent instanceof String json) {
      var javaType = objectMapper.getTypeFactory()
              .constructParametricType(BaseEvent.class, ApplicationPayload.class);
      return objectMapper.readValue(json, javaType);
    }

    String json = objectMapper.writeValueAsString(rawEvent);
    var javaType = objectMapper.getTypeFactory()
            .constructParametricType(BaseEvent.class, ApplicationPayload.class);
    return objectMapper.readValue(json, javaType);
  }
}
