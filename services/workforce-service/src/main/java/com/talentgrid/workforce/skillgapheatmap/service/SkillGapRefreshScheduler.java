package com.talentgrid.workforce.skillgapheatmap.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SkillGapRefreshScheduler {

    private final SkillGapService skillGapService;

    @Scheduled(fixedDelayString = "${skill-gap.bench-poll-ms:300000}")
    public void pollBenchSkills() {
        try {
            var response = skillGapService.refreshBenchFromDatabase();
            log.debug("[SKILL-GAP] Scheduled bench poll — status={}, skills={}",
                    response.getStatus(), response.getProcessedSkills());
        } catch (Exception ex) {
            log.warn("[SKILL-GAP] Scheduled bench poll failed: {}", ex.getMessage());
        }
    }
}
