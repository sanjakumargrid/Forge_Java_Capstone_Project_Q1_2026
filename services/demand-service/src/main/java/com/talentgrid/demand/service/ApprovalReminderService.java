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

import java.util.List;
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
                Map.ofEntries(
                        Map.entry("demandId",       String.valueOf(demand.getDemandId())),
                        Map.entry("demandTitle",    demand.getTitle() != null ? demand.getTitle() : ""),
                        Map.entry("elapsedHours",   String.valueOf(elapsedHours)),
                        Map.entry("creatorName",    demand.getCreatorName() != null ? demand.getCreatorName() : "Unknown"),
                        Map.entry("recipientRole",  "Project Manager"),
                        Map.entry("projectName",    demand.getProjectName() != null ? demand.getProjectName() : ""),
                        Map.entry("mandatorySkills", formatMandatorySkills(demand)),
                        Map.entry("optionalSkills",  formatOptionalSkills(demand))
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
                  Map.ofEntries(
                          Map.entry("demandId",       String.valueOf(demand.getDemandId())),
                          Map.entry("demandTitle",    demand.getTitle() != null ? demand.getTitle() : ""),
                          Map.entry("elapsedHours",   String.valueOf(elapsedHours)),
                          Map.entry("creatorName",    demand.getCreatorName() != null ? demand.getCreatorName() : "Unknown"),
                          Map.entry("recipientRole",  "Demand Creator"),
                          Map.entry("projectName",    demand.getProjectName() != null ? demand.getProjectName() : ""),
                          Map.entry("mandatorySkills", formatMandatorySkills(demand)),
                          Map.entry("optionalSkills",  formatOptionalSkills(demand))
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

  /**
   * Sends 72-hour escalation to PM (action required) and Creator (information)
   * before the demand is auto-closed for SLA breach.
   */
  public boolean sendApprovalEscalation(
          Demand demand,
          ApprovalSlaScheduler.PmInfo pm,
          long elapsedHours) {

    boolean anySent = false;
    String correlationId = UUID.randomUUID().toString();

    if (pm.userId() != null && pm.email() != null) {
      try {
        notificationEventPublisher.sendInAppAndEmail(
                String.valueOf(pm.userId()),
                pm.email(),
                pm.slackId(),
                "DEMAND_APPROVAL_ESCALATION",
                "Urgent: Demand Approval Overdue — " + elapsedHours + "h",
                String.format(
                        "Demand '%s' (ID: %d) has been awaiting your approval for %d hours. "
                                + "It will be auto-closed imminently if no action is taken.",
                        demand.getTitle(),
                        demand.getDemandId(),
                        elapsedHours),
                "demand-service",
                String.valueOf(demand.getDemandId()),
                "DEMAND",
                "HIGH",
                "demand-approval-reminder",
                Map.ofEntries(
                        Map.entry("demandId", String.valueOf(demand.getDemandId())),
                        Map.entry("demandTitle", demand.getTitle() != null ? demand.getTitle() : ""),
                        Map.entry("elapsedHours", String.valueOf(elapsedHours)),
                        Map.entry("creatorName", demand.getCreatorName() != null ? demand.getCreatorName() : "Unknown"),
                        Map.entry("recipientRole", "Project Manager"),
                        Map.entry("projectName", demand.getProjectName() != null ? demand.getProjectName() : ""),
                        Map.entry("mandatorySkills", formatMandatorySkills(demand)),
                        Map.entry("optionalSkills", formatOptionalSkills(demand))
                ),
                correlationId
        );
        log.info("[ESCALATION] 72h escalation sent to PM userId={} for demandId={}",
                pm.userId(), demand.getDemandId());
        anySent = true;
      } catch (Exception e) {
        log.error("[ESCALATION] Failed to notify PM for demandId={}: {}",
                demand.getDemandId(), e.getMessage(), e);
      }
    } else {
      log.warn("[ESCALATION] PM info not available for demandId={} — PM escalation skipped",
              demand.getDemandId());
    }

    if (demand.getCreatedBy() != null) {
      try {
        UserDto creator = userAuthServiceClient.getUserById(demand.getCreatedBy());
        if (creator != null && creator.getEmail() != null) {
          notificationEventPublisher.sendInAppAndEmail(
                  String.valueOf(creator.getId()),
                  creator.getEmail(),
                  creator.getSlackId(),
                  "DEMAND_APPROVAL_ESCALATION",
                  "Urgent: Your Demand Approval is Overdue",
                  String.format(
                          "Your demand '%s' (ID: %d) has been in PENDING_APPROVAL for %d hours. "
                                  + "It will be auto-closed shortly if the project manager does not act.",
                          demand.getTitle(),
                          demand.getDemandId(),
                          elapsedHours),
                  "demand-service",
                  String.valueOf(demand.getDemandId()),
                  "DEMAND",
                  "HIGH",
                  "demand-approval-reminder",
                  Map.ofEntries(
                          Map.entry("demandId", String.valueOf(demand.getDemandId())),
                          Map.entry("demandTitle", demand.getTitle() != null ? demand.getTitle() : ""),
                          Map.entry("elapsedHours", String.valueOf(elapsedHours)),
                          Map.entry("creatorName", demand.getCreatorName() != null ? demand.getCreatorName() : "Unknown"),
                          Map.entry("recipientRole", "Demand Creator"),
                          Map.entry("projectName", demand.getProjectName() != null ? demand.getProjectName() : ""),
                          Map.entry("mandatorySkills", formatMandatorySkills(demand)),
                          Map.entry("optionalSkills", formatOptionalSkills(demand))
                  ),
                  correlationId
          );
          log.info("[ESCALATION] 72h escalation sent to Creator userId={} for demandId={}",
                  creator.getId(), demand.getDemandId());
          anySent = true;
        }
      } catch (Exception e) {
        log.error("[ESCALATION] Failed to notify Creator for demandId={}: {}",
                demand.getDemandId(), e.getMessage(), e);
      }
    }

    try {
      auditLogClient.logAction(AuditLogPayload.builder()
              .entityType("DEMAND")
              .entityId(demand.getDemandId())
              .action(AuditAction.STATUS_CHANGE)
              .actorId(0L)
              .beforeState(null)
              .afterState(Map.of(
                      "event", "APPROVAL_SLA_72H_ESCALATION",
                      "status", demand.getStatus().name(),
                      "demandId", demand.getDemandId(),
                      "elapsedHours", elapsedHours
              ))
              .serviceName("demand-service")
              .endpoint("/scheduler/approval-sla")
              .build());
    } catch (Exception e) {
      log.warn("[ESCALATION] Audit log failed for demandId={}: {}", demand.getDemandId(), e.getMessage());
    }

    return anySent;
  }

  // ─────────────────────────────────────────────────────────────────────────────
  // Skill formatting helpers
  // ─────────────────────────────────────────────────────────────────────────────

  private String formatMandatorySkills(Demand demand) {
    if (demand.getDemandSkills() == null) return "None specified";
    List<String> skills = demand.getDemandSkills().stream()
            .filter(ds -> Boolean.TRUE.equals(ds.getIsMandatory()))
            .map(ds -> ds.getSkill().getSkillName())
            .toList();
    return skills.isEmpty() ? "None specified" : String.join(", ", skills);
  }

  private String formatOptionalSkills(Demand demand) {
    if (demand.getDemandSkills() == null) return "None specified";
    List<String> skills = demand.getDemandSkills().stream()
            .filter(ds -> !Boolean.TRUE.equals(ds.getIsMandatory()))
            .map(ds -> ds.getSkill().getSkillName())
            .toList();
    return skills.isEmpty() ? "None specified" : String.join(", ", skills);
  }
}