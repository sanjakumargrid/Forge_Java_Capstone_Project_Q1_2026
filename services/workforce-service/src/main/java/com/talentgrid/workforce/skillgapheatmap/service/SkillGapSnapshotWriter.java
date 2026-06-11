package com.talentgrid.workforce.skillgapheatmap.service;

import com.talentgrid.workforce.skillgapheatmap.entity.SkillGapAnalytics;
import com.talentgrid.workforce.skillgapheatmap.repository.SkillGapAnalyticsQueryRepository;
import com.talentgrid.workforce.skillgapheatmap.repository.SkillGapAnalyticsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class SkillGapSnapshotWriter {

    private final SkillGapAnalyticsRepository repository;
    private final SkillGapAnalyticsQueryRepository queryRepository;

    @Transactional(readOnly = true)
    public Map<String, Integer> fetchPreviousGapScores() {
        return queryRepository.getLatestSnapshot().stream()
                .collect(Collectors.toMap(
                        SkillGapAnalytics::getSkillName,
                        SkillGapAnalytics::getGapScore,
                        (a, b) -> a,
                        () -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER)
                ));
    }

    @Transactional
    public void persistSnapshot(List<SkillGapAnalytics> rows, LocalDateTime snapshotTime, int retentionDays) {
        repository.saveAll(rows);

        LocalDateTime cutoff = snapshotTime.minusDays(retentionDays);
        int deleted = repository.deleteByCalculatedAtBefore(cutoff);
        if (deleted > 0) {
            log.info("[SKILL-GAP] Data retention: deleted {} rows older than {} days.", deleted, retentionDays);
        }
    }
}
