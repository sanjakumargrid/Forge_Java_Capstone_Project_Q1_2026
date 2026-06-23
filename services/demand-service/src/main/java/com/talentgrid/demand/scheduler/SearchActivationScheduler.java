package com.talentgrid.demand.scheduler;

import com.talentgrid.demand.service.DemandLifecycleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job that auto-activates approved demands.
 *
 * <p>After PM approval the demand remains in {@code APPROVED} until this job runs,
 * then transitions to:
 * <ul>
 *   <li>{@code INTERNAL_SEARCH} — default internal bench search</li>
 *   <li>{@code OPEN_EXTERNAL} — when {@code benchHiring} is true</li>
 * </ul>
 *
 * <p>Idempotent: only demands still in {@code APPROVED} are processed.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SearchActivationScheduler {

    private final DemandLifecycleService lifecycleService;

  @Scheduled(cron = "${demand.scheduler.search-activation.cron:0 * * * * *}")
  public void activateApprovedDemands() {
        log.debug("[SEARCH-ACTIVATION] Starting approved-demand activation check");

        int activated = lifecycleService.activateApprovedDemands();

        if (activated > 0) {
            log.info("[SEARCH-ACTIVATION] Activated {} approved demand(s)", activated);
        } else {
            log.debug("[SEARCH-ACTIVATION] No approved demands to activate");
        }
    }
}
