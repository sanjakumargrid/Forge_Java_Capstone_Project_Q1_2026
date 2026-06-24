package com.talentgrid.workforce.skillgapheatmap.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SkillGapRefreshScheduler {

    private final SkillGapService skillGapService;

    // Fires after Tomcat is fully ready so the initial snapshot is calculated on startup.
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        try {
            var response = skillGapService.refresh();
            log.info("[SKILL-GAP] Initial heatmap snapshot on startup — status={}, skills={}.",
                    response.getStatus(), response.getProcessedSkills());
        } catch (Exception ex) {
            log.warn("[SKILL-GAP] Startup refresh failed — heatmap will be empty until demand-service is available. Reason: {}",
                    ex.getMessage());
        }
    }

    // Daily refresh at 07:00 UTC (after BenchReport refreshes at 06:00).
    @Scheduled(cron = "0 0 7 * * *", zone = "UTC")
    public void scheduledRefresh() {
        log.info("[SKILL-GAP] Running scheduled heatmap refresh.");
        try {
            var response = skillGapService.refresh();
            log.info("[SKILL-GAP] Scheduled refresh finished — status={}, skills={}.",
                    response.getStatus(), response.getProcessedSkills());
        } catch (Exception ex) {
            log.error("[SKILL-GAP] Scheduled refresh failed: {}", ex.getMessage());
        }
    }
}
