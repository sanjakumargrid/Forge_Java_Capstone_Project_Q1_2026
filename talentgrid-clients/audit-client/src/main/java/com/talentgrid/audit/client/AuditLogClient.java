package com.talentgrid.audit.client;

import com.talentgrid.audit.constants.AuditConstants;
import com.talentgrid.audit.dto.AuditLogPayload;
import com.talentgrid.kafka.events.base.BaseEvent;
import com.talentgrid.kafka.producer.KafkaProducerService;
import com.talentgrid.kafka.topics.TalentGridTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLogClient {

  private static final String AUDIT_TOPIC =
          TalentGridTopics.SYSTEM_EVENTS;

  private final KafkaProducerService kafkaProducerService;

  public void logAction(
          AuditLogPayload payload
  ) {

    String correlationId =
            payload.getTraceId() != null
                    ? payload.getTraceId()
                    : UUID.randomUUID().toString();

    BaseEvent<AuditLogPayload> event =
            BaseEvent.<AuditLogPayload>builder()
                    .eventType(
                            AuditConstants.AUDIT_EVENT_TYPE
                    )
                    .source(
                            payload.getServiceName()
                    )
                    .correlationId(
                            correlationId
                    )
                    .payload(
                            payload
                    )
                    .build();

    kafkaProducerService.sendEvent(
            AUDIT_TOPIC,
            correlationId,
            event
    );

    log.info(
            "Audit event published | entityType={} | entityId={} | action={}",
            payload.getEntityType(),
            payload.getEntityId(),
            payload.getAction()
    );
  }
}