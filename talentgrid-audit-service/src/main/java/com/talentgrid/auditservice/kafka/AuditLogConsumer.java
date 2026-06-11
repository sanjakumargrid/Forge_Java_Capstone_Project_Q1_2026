package com.talentgrid.auditservice.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talentgrid.audit.dto.AuditLogPayload;
import com.talentgrid.auditservice.audit.AuditLogService;
import com.talentgrid.kafka.events.base.BaseEvent;
import com.talentgrid.kafka.topics.TalentGridTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLogConsumer {

  private final AuditLogService auditLogService;
  private final ObjectMapper objectMapper;

  @KafkaListener(
          topics = TalentGridTopics.SYSTEM_EVENTS,
          groupId = "audit-service-group"
  )
  public void consumeAuditLogEvent(BaseEvent<?> event) {

    try {

      AuditLogPayload payload = objectMapper.convertValue(
              event.getPayload(),
              AuditLogPayload.class
      );

      log.info(
              "Received audit event | eventType={} | entityType={} | entityId={}",
              event.getEventType(),
              payload.getEntityType(),
              payload.getEntityId()
      );

      auditLogService.saveAuditLog(payload);

      log.info(
              "Audit event processed successfully | eventId={}",
              event.getEventId()
      );

    } catch (Exception ex) {

      log.error(
              "Failed to process audit event | eventId={}",
              event.getEventId(),
              ex
      );

      throw ex;
    }
  }
}