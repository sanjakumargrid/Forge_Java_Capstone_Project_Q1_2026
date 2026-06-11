package com.talentgrid.demand.service;

import com.talentgrid.audit.client.AuditLogClient;
import com.talentgrid.audit.dto.AuditAction;
import com.talentgrid.audit.dto.AuditLogPayload;
import com.talentgrid.demand.constants.DemandConstants;
import com.talentgrid.demand.kafka.DemandKafkaProducer;
import com.talentgrid.kafka.events.demand.DemandPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DemandService {

  private final DemandKafkaProducer demandKafkaProducer;
  private final AuditLogClient auditLogClient;

  public String createDemand(
          DemandPayload request,
          String requestId
  ) {

    DemandPayload payload = DemandPayload.builder()
            .demandId(UUID.randomUUID().toString())
            .title(request.getTitle())
            .location(request.getLocation())
            .level(
                    request.getLevel() != null
                            ? request.getLevel()
                            : DemandConstants.DEFAULT_LEVEL
            )
            .skills(
                    request.getSkills() != null
                            ? request.getSkills()
                            : DemandConstants.DEFAULT_SKILLS
            )
            .status(DemandConstants.DEFAULT_STATUS)
            .raisedBy(request.getRaisedBy())
            .recipientEmail(request.getRecipientEmail())
            .build();

    demandKafkaProducer.publishDemandCreated(
            payload,
            requestId
    );

    AuditLogPayload auditPayload =
            AuditLogPayload.builder()
                    .entityType(DemandConstants.ENTITY_DEMAND)
                    .entityId(DemandConstants.DEFAULT_ENTITY_ID)
                    .action(AuditAction.CREATE)
                    .actorId(DemandConstants.DEFAULT_ACTOR_ID)
                    .beforeState(null)
                    .afterState(Map.of(
                                    "title", payload.getTitle(),
                                    "status", payload.getStatus()))
                    .traceId(requestId)
                    .serviceName(DemandConstants.SERVICE_NAME)
                    .endpoint(DemandConstants.CREATE_DEMAND_ENDPOINT)
                    .ipAddress(DemandConstants.DEFAULT_IP_ADDRESS)
                    .userAgent(DemandConstants.DEFAULT_USER_AGENT)
                    .build();

    auditLogClient.logAction(
            auditPayload
    );

    return "✅ DEMAND_CREATED event published to Kafka. Check Kafka UI at http://localhost:7777";
  }
}