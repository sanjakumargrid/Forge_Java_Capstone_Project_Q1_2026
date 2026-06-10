package com.talentgrid.audit.client;

import com.talentgrid.audit.dto.AuditAction;
import com.talentgrid.audit.dto.AuditLogPayload;
import com.talentgrid.kafka.events.base.BaseEvent;
import com.talentgrid.kafka.producer.KafkaProducerService;
import com.talentgrid.kafka.topics.TalentGridTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLogClient {

  private static final String AUDIT_TOPIC = TalentGridTopics.SYSTEM_EVENTS;

  private final KafkaProducerService kafkaProducerService;

  public void logAction(
          String entityType,
          Long entityId,
          AuditAction action,
          Long actorId,
          Map<String, Object> beforeState,
          Map<String, Object> afterState,
          String traceId,
          String serviceName,
          String endpoint,
          String ipAddress,
          String userAgent
  ) {

    AuditLogPayload payload = AuditLogPayload.builder()
            .entityType(entityType)
            .entityId(entityId)
            .action(action)
            .actorId(actorId)
            .beforeState(beforeState)
            .afterState(afterState)
            .traceId(traceId)
            .serviceName(serviceName)
            .endpoint(endpoint)
            .ipAddress(ipAddress)
            .userAgent(userAgent)
            .build();

    BaseEvent<AuditLogPayload> event =
            BaseEvent.<AuditLogPayload>builder()
                    .eventType("AUDIT_LOG_ENTRY")
                    .source(serviceName)
                    .correlationId(
                            traceId != null
                                    ? traceId
                                    : UUID.randomUUID().toString()
                    )
                    .payload(payload)
                    .build();

    kafkaProducerService.sendEvent(
            AUDIT_TOPIC,
            event
    );

    log.info(
            "Audit event published | entityType={} | entityId={} | action={}",
            entityType,
            entityId,
            action
    );
  }
}