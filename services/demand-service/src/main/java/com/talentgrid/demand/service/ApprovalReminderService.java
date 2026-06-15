package com.talentgrid.demand.service;

import com.talentgrid.audit.client.AuditLogClient;
import com.talentgrid.audit.dto.AuditAction;
import com.talentgrid.audit.dto.AuditLogPayload;
import com.talentgrid.clients.notification.NotificationEventPublisher;
import com.talentgrid.demand.client.UserAuthServiceClient;
import com.talentgrid.demand.client.dto.UserDto;
import com.talentgrid.demand.client.dto.UserSummaryResponse;
import com.talentgrid.demand.domain.entity.Demand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * Handles approval SLA reminder notifications.
 *
 * Sends reminders when a demand remains in
 * PENDING_APPROVAL for more than 72 hours.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApprovalReminderService {

  private final UserAuthServiceClient userAuthServiceClient;
  private final NotificationEventPublisher notificationEventPublisher;
  private final AuditLogClient auditLogClient;

  /**
   * Sends approval reminder notifications to:
   * 1. RMG responsible for the demand location
   * 2. Hiring Manager (creator)
   */
  public boolean sendApprovalReminder(
          Demand demand,
          long elapsedHours) {

    try {

      String correlationId = UUID.randomUUID().toString();

      /*
       * =====================================================
       * Notify RMG
       * =====================================================
       */
      UserSummaryResponse rmg =
              userAuthServiceClient.getRmgByLocation(
                      demand.getLocation());

      if (rmg != null) {

        notificationEventPublisher.sendInAppAndEmail(
                String.valueOf(rmg.getId()),
                rmg.getEmail(),
                rmg.getSlackId(),   // <-- NEW PARAMETER
                "DEMAND_APPROVAL_SLA_REMINDER",
                "Demand Approval Pending",
                String.format(
                        "Demand '%s' has been awaiting approval for %d hours.",
                        demand.getTitle(),
                        elapsedHours),
                "demand-service",
                String.valueOf(demand.getDemandId()),
                "DEMAND",
                "HIGH",
                null,
                Map.of(
                        "demandId", String.valueOf(demand.getDemandId()),
                        "demandTitle", demand.getTitle(),
                        "elapsedHours", String.valueOf(elapsedHours)
                ),
                correlationId
        );

        log.info(
                "Approval SLA reminder sent to RMG {} for demand {}",
                rmg.getEmail(),
                demand.getDemandId());
      }

      /*
       * =====================================================
       * Notify Hiring Manager
       * =====================================================
       */
      if (demand.getCreatedBy() != null) {

        UserDto hm =
                userAuthServiceClient.getUserById(
                        demand.getCreatedBy());

        if (hm != null) {

          notificationEventPublisher.sendInAppAndEmail(
                  String.valueOf(hm.getId()),
                  hm.getEmail(),
                  hm.getSlackId(),    // <-- NEW PARAMETER
                  "DEMAND_APPROVAL_SLA_REMINDER",
                  "Demand Approval Delayed",
                  String.format(
                          "Your demand '%s' is still awaiting approval after %d hours.",
                          demand.getTitle(),
                          elapsedHours),
                  "demand-service",
                  String.valueOf(demand.getDemandId()),
                  "DEMAND",
                  "HIGH",
                  null,
                  Map.of(
                          "demandId", String.valueOf(demand.getDemandId()),
                          "demandTitle", demand.getTitle(),
                          "elapsedHours", String.valueOf(elapsedHours)
                  ),
                  correlationId
          );

          log.info(
                  "Approval SLA reminder sent to Hiring Manager {} for demand {}",
                  hm.getEmail(),
                  demand.getDemandId());
        }
      }

      /*
       * =====================================================
       * Audit Log
       * =====================================================
       */
      auditLogClient.logAction(
              AuditLogPayload.builder()
                      .entityType("DEMAND")
                      .entityId(demand.getDemandId())
                      .action(AuditAction.STATUS_CHANGE)
                      .actorId(0L)
                      .beforeState(null)
                      .afterState(Map.of(
                              "event", "APPROVAL_SLA_REMINDER",
                              "status", demand.getStatus().name(),
                              "demandId", demand.getDemandId(),
                              "elapsedHours", elapsedHours
                      ))
                      .serviceName("demand-service")
                      .endpoint("/scheduler/approval-sla")
                      .build()
      );

      log.info(
              "Approval SLA audit recorded for demand {}",
              demand.getDemandId());

      return true;

    } catch (Exception ex) {

      log.error(
              "Failed to process SLA reminder for demand {}",
              demand.getDemandId(),
              ex);

      return false;
    }
  }
}