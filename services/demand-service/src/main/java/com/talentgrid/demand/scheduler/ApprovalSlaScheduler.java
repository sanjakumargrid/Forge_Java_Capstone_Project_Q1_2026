package com.talentgrid.demand.scheduler;

import com.talentgrid.demand.domain.entity.Demand;
import com.talentgrid.demand.domain.entity.DemandStatusHistory;
import com.talentgrid.demand.domain.enums.DemandStatus;
import com.talentgrid.demand.repository.DemandRepository;
import com.talentgrid.demand.repository.DemandStatusHistoryRepository;
import com.talentgrid.demand.service.ApprovalReminderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApprovalSlaScheduler {

  private static final long APPROVAL_SLA_HOURS = 72L;

  private final DemandRepository demandRepository;
  private final DemandStatusHistoryRepository historyRepository;
  private final ApprovalReminderService approvalReminderService;

  /**
   * Runs every hour.
   *
   * Checks all demands currently in PENDING_APPROVAL and
   * triggers reminder notifications if approval SLA exceeds 72 hours.
   */
  @Scheduled(cron = "0 0 * * * *")
  public void checkApprovalSla() {

    log.info("Starting approval SLA check");

    List<Demand> pendingDemands =
            demandRepository.findByStatusAndIsDeletedFalse(
                    DemandStatus.PENDING_APPROVAL);

    if (pendingDemands.isEmpty()) {
      log.debug("No pending approval demands found");
      return;
    }

    for (Demand demand : pendingDemands) {

      try {

        Optional<DemandStatusHistory> pendingApprovalHistory =
                historyRepository
                        .findTopByDemandDemandIdAndToStatusOrderByChangedAtDesc(
                                demand.getDemandId(),
                                DemandStatus.PENDING_APPROVAL);

        if (pendingApprovalHistory.isEmpty()) {

          log.warn(
                  "Skipping demand {} because no PENDING_APPROVAL history record was found",
                  demand.getDemandId());

          continue;
        }

        OffsetDateTime pendingSince =
                pendingApprovalHistory.get().getChangedAt();

        long elapsedHours =
                Duration.between(
                                pendingSince,
                                OffsetDateTime.now())
                        .toHours();

        if (elapsedHours < APPROVAL_SLA_HOURS) {
          continue;
        }

        log.info(
                "Approval SLA breached for demandId={} ({} hours pending)",
                demand.getDemandId(),
                elapsedHours);

        approvalReminderService.sendApprovalReminder(
                demand,
                elapsedHours);

      } catch (Exception ex) {

        log.error(
                "Failed processing approval SLA for demandId={}",
                demand.getDemandId(),
                ex);
      }
    }

    log.info("Approval SLA check completed");
  }
}