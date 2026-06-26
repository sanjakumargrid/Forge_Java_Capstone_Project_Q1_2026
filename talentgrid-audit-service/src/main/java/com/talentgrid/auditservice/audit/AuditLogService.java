package com.talentgrid.auditservice.audit;

import com.talentgrid.audit.dto.AuditLogPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

  private final AuditLogRepository auditLogRepository;

  public void saveAuditLog(AuditLogPayload payload) {

    try {

      AuditLogEntity auditLog = AuditLogEntity.builder()
              .entityType(payload.getEntityType())
              .entityId(payload.getEntityId())
              .action(payload.getAction())
              .actorId(payload.getActorId())
              .beforeState(payload.getBeforeState())
              .afterState(payload.getAfterState())
              .reasonForEdit(payload.getReasonForEdit())
              .traceId(payload.getTraceId())
              .serviceName(payload.getServiceName())
              .endpoint(payload.getEndpoint())
              .ipAddress(payload.getIpAddress())
              .userAgent(payload.getUserAgent())
              .build();

      AuditLogEntity savedAuditLog = auditLogRepository.save(auditLog);

      log.info(
              "Audit log persisted successfully | auditId={} | entityType={} | entityId={} | action={}",
              savedAuditLog.getId(),
              savedAuditLog.getEntityType(),
              savedAuditLog.getEntityId(),
              savedAuditLog.getAction()
      );

    } catch (Exception ex) {

      log.error(
              "Failed to persist audit log | entityType={} | entityId={}",
              payload.getEntityType(),
              payload.getEntityId(),
              ex
      );

      throw new RuntimeException(
              "Failed to persist audit log",
              ex
      );
    }
  }
}