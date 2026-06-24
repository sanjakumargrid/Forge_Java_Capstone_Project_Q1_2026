package com.talentgrid.candidate.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talentgrid.clients.notification.NotificationEventPublisher;
import com.talentgrid.kafka.consumer.BaseKafkaConsumer;
import com.talentgrid.kafka.events.base.BaseEvent;
import com.talentgrid.kafka.events.candidate.CandidatePayload;
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
public class CandidateEventTranslator extends BaseKafkaConsumer<CandidatePayload> {

  private final NotificationEventPublisher notificationEventPublisher;
  private final ObjectMapper objectMapper;

  @KafkaListener(
          topics = TalentGridTopics.CANDIDATE_EVENTS,
          groupId = "${spring.kafka.consumer.group-id:candidate-service-group}",
          concurrency = "3",
          containerFactory = "kafkaListenerContainerFactory"
  )
  public void onMessage(
          @Payload Object rawEvent,
          @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
          @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
          @Header(KafkaHeaders.OFFSET) long offset) {

    log.info("[CANDIDATE-TRANSLATOR] ▶ Message received | topic={} | partition={} | offset={}",
            topic, partition, offset);

    try {
      BaseEvent<CandidatePayload> event = extractPayload(rawEvent);

      if (event == null || event.getPayload() == null) {
        log.warn("[CANDIDATE-TRANSLATOR] Null event or payload at offset={} — skipping", offset);
        return;
      }

      process(event);

    } catch (Exception e) {
      log.error("[CANDIDATE-TRANSLATOR] ✗ Failed to process message at offset={} | error={}",
              offset, e.getMessage(), e);
    }
  }

  @Override
  protected void handleEvent(BaseEvent<CandidatePayload> event) {
    CandidatePayload candidate = event.getPayload();
    String eventType = event.getEventType();
    String correlationId = event.getCorrelationId();

    log.info("[CANDIDATE-TRANSLATOR] Translating event | type={} | candidateId={}",
            eventType, candidate.getCandidateId());

    switch (eventType) {
      case "CANDIDATE_CREATED" -> translateCandidateCreated(candidate, correlationId);
      case "CANDIDATE_UPDATED" -> translateCandidateUpdated(candidate, correlationId);
      case "CANDIDATE_DELETED" -> translateCandidateDeleted(candidate, correlationId);
      default -> log.debug("[CANDIDATE-TRANSLATOR] No notification mapping for eventType='{}' — skipping", eventType);
    }
  }

  // ─────────────────────────────────────────────────────────
  // Translation Methods
  // ─────────────────────────────────────────────────────────

  private void translateCandidateCreated(CandidatePayload candidate, String correlationId) {
    if (candidate.getEmail() == null || candidate.getEmail().isBlank()) {
      log.warn("[CANDIDATE-TRANSLATOR] ⚠ email is null/blank for candidateId={} — email notification will be skipped",
              candidate.getCandidateId());
    }

    String fullName = fullName(candidate);
    String title = "Welcome to TalentGrid, " + fullName + "!";
    String message = String.format(
            "Your candidate profile has been successfully created. " +
                    "We will keep you updated on matching opportunities. " +
                    "Candidate ID: %s.",
            candidate.getCandidateId()
    );

    notificationEventPublisher.sendInAppAndEmail(
            candidate.getCandidateId() != null ? candidate.getCandidateId().toString() : null,
            candidate.getEmail(),
            null,
            "CANDIDATE_CREATED",
            title,
            message,
            "candidate-service",
            candidate.getCandidateId() != null ? candidate.getCandidateId().toString() : null,
            "CANDIDATE",
            "NORMAL",
            "candidate-created",
            Map.of(
                    "candidateName", fullName,
                    "candidateId",   candidate.getCandidateId() != null ? candidate.getCandidateId().toString() : ""
            ),
            correlationId
    );

    log.info("[CANDIDATE-TRANSLATOR] ✓ NOTIFICATION_SEND published | candidateId={} | type=CANDIDATE_CREATED",
            candidate.getCandidateId());
  }

  private void translateCandidateUpdated(CandidatePayload candidate, String correlationId) {
    String fullName = fullName(candidate);
    String title = "Your profile has been updated";
    String message = String.format(
            "Your TalentGrid candidate profile (%s) has been updated successfully.",
            fullName
    );

    notificationEventPublisher.sendInAppAndEmail(
            candidate.getCandidateId() != null ? candidate.getCandidateId().toString() : null,
            candidate.getEmail(),
            null,
            "CANDIDATE_UPDATED",
            title,
            message,
            "candidate-service",
            candidate.getCandidateId() != null ? candidate.getCandidateId().toString() : null,
            "CANDIDATE",
            "NORMAL",
            "candidate-updated",
            Map.of(
                    "candidateName", fullName
            ),
            correlationId
    );

    log.info("[CANDIDATE-TRANSLATOR] ✓ NOTIFICATION_SEND published | candidateId={} | type=CANDIDATE_UPDATED",
            candidate.getCandidateId());
  }

  private void translateCandidateDeleted(CandidatePayload candidate, String correlationId) {
    String fullName = fullName(candidate);
    String title = "Your profile has been removed";
    String message = String.format(
            "Your TalentGrid candidate profile (%s) has been deleted. Reason: %s.",
            fullName,
            candidate.getDeleteReason() != null ? candidate.getDeleteReason() : "Not specified"
    );

    notificationEventPublisher.sendInAppAndEmail(
            candidate.getCandidateId() != null ? candidate.getCandidateId().toString() : null,
            candidate.getEmail(),
            null,
            "CANDIDATE_DELETED",
            title,
            message,
            "candidate-service",
            candidate.getCandidateId() != null ? candidate.getCandidateId().toString() : null,
            "CANDIDATE",
            "HIGH",
            "candidate-deleted",
            Map.of(
                    "candidateName",  fullName,
                    "deleteReason",   candidate.getDeleteReason() != null ? candidate.getDeleteReason() : "N/A"
            ),
            correlationId
    );

    log.info("[CANDIDATE-TRANSLATOR] ✓ NOTIFICATION_SEND published | candidateId={} | type=CANDIDATE_DELETED",
            candidate.getCandidateId());
  }

  // ─────────────────────────────────────────────────────────
  // Helpers
  // ─────────────────────────────────────────────────────────

  private String fullName(CandidatePayload c) {
    String first = c.getFirstName() != null ? c.getFirstName() : "";
    String last  = c.getLastName()  != null ? c.getLastName()  : "";
    return (first + " " + last).trim();
  }

  @SuppressWarnings("unchecked")
  private BaseEvent<CandidatePayload> extractPayload(Object rawEvent) throws Exception {

    if (rawEvent instanceof BaseEvent<?> base) {
      if (base.getPayload() instanceof CandidatePayload) {
        return (BaseEvent<CandidatePayload>) base;
      }
      CandidatePayload payload = objectMapper.convertValue(base.getPayload(), CandidatePayload.class);
      return BaseEvent.<CandidatePayload>builder()
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
                .constructParametricType(BaseEvent.class, CandidatePayload.class);
        return objectMapper.readValue(json, javaType);
      }
      String json = objectMapper.writeValueAsString(value);
      var javaType = objectMapper.getTypeFactory()
              .constructParametricType(BaseEvent.class, CandidatePayload.class);
      return objectMapper.readValue(json, javaType);
    }

    if (rawEvent instanceof String json) {
      var javaType = objectMapper.getTypeFactory()
              .constructParametricType(BaseEvent.class, CandidatePayload.class);
      return objectMapper.readValue(json, javaType);
    }

    String json = objectMapper.writeValueAsString(rawEvent);
    var javaType = objectMapper.getTypeFactory()
            .constructParametricType(BaseEvent.class, CandidatePayload.class);
    return objectMapper.readValue(json, javaType);
  }
}