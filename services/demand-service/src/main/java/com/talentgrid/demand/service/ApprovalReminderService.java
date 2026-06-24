package com.talentgrid.demand.service;

import com.talentgrid.audit.client.AuditLogClient;
import com.talentgrid.audit.dto.AuditAction;
import com.talentgrid.audit.dto.AuditLogPayload;
import com.talentgrid.clients.notification.NotificationEventPublisher;
import com.talentgrid.demand.client.UserAuthServiceClient;
import com.talentgrid.demand.client.dto.UserDto;
import com.talentgrid.demand.domain.entity.Demand;
import com.talentgrid.demand.scheduler.ApprovalSlaScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * Sends approval SLA reminder notifications (24-hour mark).
 *
 * Notifies:
 * - Project Manager (action required) — resolved via projectId → Project → projectManagerId → User
 * - Demand Creator (information)
 *
 * Channels: Email, In-App, Slack (Slack is feature-toggled).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApprovalReminderService {

  private final UserAuthServiceClient userAuthServiceClient;
  private final NotificationEventPublisher notificationEventPublisher;
  private final AuditLogClient auditLogClient;

  /**
   * Sends 24-hour reminder to PM (action required) and Creator (information).
   *
   * @param demand      the demand in PENDING_APPROVAL
   * @param pm          pre-resolved PM info (from ApprovalSlaScheduler)
   * @param elapsedHours hours elapsed since entering PENDING_APPROVAL
   * @return true if at least one notification was dispatched without exception
   */
  public boolean sendApprovalReminder(
          Demand demand,
          ApprovalSlaScheduler.PmInfo pm,
          long elapsedHours) {

    boolean anySent = false;
    String correlationId = UUID.randomUUID().toString();

    // ── Notify PM (Action Required) ──────────────────────────────────────────
    if (pm.userId() != null && pm.email() != null) {
      try {
        notificationEventPublisher.sendInAppAndEmail(
                String.valueOf(pm.userId()),
                pm.email(),
                pm.slackId(),
                "DEMAND_APPROVAL_REMINDER",
                "Action Required: Demand Approval Pending",
                String.format(
                        "Demand '%s' (ID: %d) has been awaiting your approval for %d hours. " +
                                "Please review and approve or reject it at the earliest.",
                        demand.getTitle(),
                        demand.getDemandId(),
                        elapsedHours),
                "demand-service",
                String.valueOf(demand.getDemandId()),
                "DEMAND",
                "HIGH",
                "demand-approval-reminder",
                Map.of(
                        "demandId",       String.valueOf(demand.getDemandId()),
                        "demandTitle",    demand.getTitle() != null ? demand.getTitle() : "",
                        "elapsedHours",   String.valueOf(elapsedHours),
                        "creatorName",    demand.getCreatorName() != null ? demand.getCreatorName() : "Unknown",
                        "recipientRole",  "Project Manager",
                        "projectName",    demand.getProjectName() != null ? demand.getProjectName() : ""
                ),
                correlationId
        );
        log.info("[REMINDER] 24h reminder sent to PM userId={} for demandId={}",
                pm.userId(), demand.getDemandId());
        anySent = true;
      } catch (Exception e) {
        log.error("[REMINDER] Failed to notify PM for demandId={}: {}",
                demand.getDemandId(), e.getMessage(), e);
      }
    } else {
      log.warn("[REMINDER] PM info not available for demandId={} — PM reminder skipped",
              demand.getDemandId());
    }

    // ── Notify Creator (Information) ─────────────────────────────────────────
    if (demand.getCreatedBy() != null) {
      try {
        UserDto creator = userAuthServiceClient.getUserById(demand.getCreatedBy());
        if (creator != null && creator.getEmail() != null) {
          notificationEventPublisher.sendInAppAndEmail(
                  String.valueOf(creator.getId()),
                  creator.getEmail(),
                  creator.getSlackId(),
                  "DEMAND_APPROVAL_REMINDER",
                  "Your Demand is Still Awaiting Approval",
                  String.format(
                          "Your demand '%s' (ID: %d) has been in PENDING_APPROVAL for %d hours. " +
                                  "The project manager has been notified.",
                          demand.getTitle(),
                          demand.getDemandId(),
                          elapsedHours),
                  "demand-service",
                  String.valueOf(demand.getDemandId()),
                  "DEMAND",
                  "NORMAL",
                  "demand-approval-reminder",
                  Map.of(
                          "demandId",       String.valueOf(demand.getDemandId()),
                          "demandTitle",    demand.getTitle() != null ? demand.getTitle() : "",
                          "elapsedHours",   String.valueOf(elapsedHours),
                          "creatorName",    demand.getCreatorName() != null ? demand.getCreatorName() : "Unknown",
                          "recipientRole",  "Demand Creator",
                          "projectName",    demand.getProjectName() != null ? demand.getProjectName() : ""
                  ),
                  correlationId
          );
          log.info("[REMINDER] 24h reminder sent to Creator userId={} for demandId={}",
                  creator.getId(), demand.getDemandId());
          anySent = true;
        }
      } catch (Exception e) {
        log.error("[REMINDER] Failed to notify Creator for demandId={}: {}",
                demand.getDemandId(), e.getMessage(), e);
      }
    }

    // ── Audit ─────────────────────────────────────────────────────────────────
    try {
      auditLogClient.logAction(AuditLogPayload.builder()
              .entityType("DEMAND")
              .entityId(demand.getDemandId())
              .action(AuditAction.STATUS_CHANGE)
              .actorId(0L) // 0 = system
              .beforeState(null)
              .afterState(Map.of(
                      "event",        "APPROVAL_SLA_24H_REMINDER",
                      "status",       demand.getStatus().name(),
                      "demandId",     demand.getDemandId(),
                      "elapsedHours", elapsedHours
              ))
              .serviceName("demand-service")
              .endpoint("/scheduler/approval-sla")
              .build());
    } catch (Exception e) {
      log.warn("[REMINDER] Audit log failed for demandId={}: {}", demand.getDemandId(), e.getMessage());
    }

    return anySent;
  }
}