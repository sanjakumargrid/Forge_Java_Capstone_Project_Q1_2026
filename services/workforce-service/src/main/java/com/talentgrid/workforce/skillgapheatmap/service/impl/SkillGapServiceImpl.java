package com.talentgrid.workforce.skillgapheatmap.service.impl;

import com.talentgrid.workforce.skillgapheatmap.dto.*;
import com.talentgrid.workforce.skillgapheatmap.entity.GapLevel;
import com.talentgrid.workforce.skillgapheatmap.entity.SkillGapAnalytics;
import com.talentgrid.workforce.skillgapheatmap.entity.TrendDirection;
import com.talentgrid.workforce.skillgapheatmap.exception.DemandServiceUnavailableException;
import com.talentgrid.workforce.skillgapheatmap.exception.SkillGapDataNotFoundException;
import com.talentgrid.workforce.skillgapheatmap.provider.DemandProvider;
import com.talentgrid.workforce.skillgapheatmap.provider.WorkforceProvider;
import com.talentgrid.workforce.skillgapheatmap.provider.model.DemandResponse;
import com.talentgrid.workforce.skillgapheatmap.provider.model.EngineerResponse;
import com.talentgrid.workforce.skillgapheatmap.repository.SkillGapAnalyticsQueryRepository;
import com.talentgrid.workforce.skillgapheatmap.service.SkillGapRefreshCoordinator;
import com.talentgrid.workforce.skillgapheatmap.service.SkillGapService;
import com.talentgrid.workforce.skillgapheatmap.service.SkillGapSnapshotWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SkillGapServiceImpl implements SkillGapService {

    private final SkillGapAnalyticsQueryRepository queryRepository;
    private final SkillGapRefreshCoordinator refreshCoordinator;
    private final SkillGapSnapshotWriter snapshotWriter;
    private final DemandProvider demandProvider;
    private final WorkforceProvider workforceProvider;

    @Value("${skill-gap.retention-days:30}")
    private int retentionDays;

    // -------------------------------------------------------------------------
    // Read endpoints
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public SkillGapResponse getSkillGap() {
        List<SkillGapAnalytics> latestSnapshot = queryRepository.getLatestSnapshot();

        if (latestSnapshot.isEmpty()) {
            throw notFoundOrUnavailable();
        }

        List<SkillGapRowDto> rows = latestSnapshot.stream()
                .map(this::mapToRow)
                .toList();

        return SkillGapResponse.builder()
                .generatedAt(queryRepository.getLatestSnapshotTime())
                .totalSkills(rows.size())
                .skills(rows)
                .degraded(refreshCoordinator.isDegraded())
                .degradedReason(refreshCoordinator.isDegraded() ? refreshCoordinator.getDegradedReason() : null)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public SkillGapSummaryResponse getSummary() {
        List<SkillGapAnalytics> latestSnapshot = queryRepository.getLatestSnapshot();

        if (latestSnapshot.isEmpty()) {
            throw notFoundOrUnavailable();
        }

        long critical = latestSnapshot.stream().filter(s -> s.getGapLevel() == GapLevel.CRITICAL).count();
        long high     = latestSnapshot.stream().filter(s -> s.getGapLevel() == GapLevel.HIGH).count();
        long medium   = latestSnapshot.stream().filter(s -> s.getGapLevel() == GapLevel.MEDIUM).count();
        long low      = latestSnapshot.stream().filter(s -> s.getGapLevel() == GapLevel.LOW).count();

        return SkillGapSummaryResponse.builder()
                .totalSkills(latestSnapshot.size())
                .criticalSkills((int) critical)
                .highSkills((int) high)
                .mediumSkills((int) medium)
                .lowSkills((int) low)
                .generatedAt(queryRepository.getLatestSnapshotTime())
                .degraded(refreshCoordinator.isDegraded())
                .degradedReason(refreshCoordinator.isDegraded() ? refreshCoordinator.getDegradedReason() : null)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public SkillTrendResponse getTrends() {
        List<SkillGapAnalytics> twoSnapshots = queryRepository.findLatestTwoSnapshotsForAllSkills();

        if (twoSnapshots.isEmpty()) {
            throw notFoundOrUnavailable();
        }

        Map<String, List<SkillGapAnalytics>> bySkill = twoSnapshots.stream()
                .collect(Collectors.groupingBy(
                        SkillGapAnalytics::getSkillName,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<SkillTrendDto> trends = new ArrayList<>();

        for (Map.Entry<String, List<SkillGapAnalytics>> entry : bySkill.entrySet()) {
            List<SkillGapAnalytics> history = entry.getValue();

            if (history.size() < 2) {
                continue;
            }

            SkillGapAnalytics current  = history.get(0);
            SkillGapAnalytics previous = history.get(1);

            trends.add(SkillTrendDto.builder()
                    .skillName(entry.getKey())
                    .previousGap(previous.getGapScore())
                    .currentGap(current.getGapScore())
                    .trendDirection(
                            current.getTrendDirection() != null
                                    ? current.getTrendDirection().name()
                                    : "STABLE")
                    .build());
        }

        if (trends.isEmpty()) {
            throw new SkillGapDataNotFoundException(
                    "Not enough snapshots to calculate trends. Please run refresh at least twice.");
        }

        return SkillTrendResponse.builder()
                .generatedAt(LocalDateTime.now())
                .trends(trends)
                .degraded(refreshCoordinator.isDegraded())
                .degradedReason(refreshCoordinator.isDegraded() ? refreshCoordinator.getDegradedReason() : null)
                .build();
    }

    // -------------------------------------------------------------------------
    // Refresh — locking, debouncing, fail-loud demand fetch
    // -------------------------------------------------------------------------

    @Override
    public RefreshResponse refresh() {
        if (refreshCoordinator.isDebounced()) {
            return refreshCoordinator.debouncedResponse();
        }

        if (!refreshCoordinator.tryAcquireLock()) {
            return refreshCoordinator.skippedInProgressResponse();
        }

        try {
            if (refreshCoordinator.isDebounced()) {
                return refreshCoordinator.debouncedResponse();
            }

            return executeRefresh();
        } catch (DemandServiceUnavailableException ex) {
            refreshCoordinator.markDegraded(ex.getMessage());
            log.error("[SKILL-GAP] Refresh failed — demand-service unavailable: {}", ex.getMessage());
            throw ex;
        } finally {
            refreshCoordinator.releaseLock();
        }
    }

    private RefreshResponse executeRefresh() {
        List<DemandResponse> demands = demandProvider.getOpenDemands();
        List<EngineerResponse> engineers = workforceProvider.getBenchEngineers();

        Map<String, Integer> demandCounts = buildDemandSkillMap(demands);
        Map<String, Integer> benchCounts  = buildBenchSkillMap(engineers);

        Set<String> allSkills = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        allSkills.addAll(demandCounts.keySet());
        allSkills.addAll(benchCounts.keySet());

        LocalDateTime snapshotTime = LocalDateTime.now();
        Map<String, Integer> previousGapScores = snapshotWriter.fetchPreviousGapScores();

        List<SkillGapAnalytics> rows = new ArrayList<>(allSkills.size());

        for (String skill : allSkills) {
            int demandCount = demandCounts.getOrDefault(skill, 0);
            int benchCount  = benchCounts.getOrDefault(skill, 0);
            int gapScore    = Math.max(demandCount - benchCount, 0);

            rows.add(SkillGapAnalytics.builder()
                    .skillName(skill)
                    .demandCount(demandCount)
                    .benchCount(benchCount)
                    .gapScore(gapScore)
                    .gapLevel(determineGapLevel(gapScore))
                    .trendDirection(determineTrend(skill, gapScore, previousGapScores))
                    .calculatedAt(snapshotTime)
                    .build());
        }

        snapshotWriter.persistSnapshot(rows, snapshotTime, retentionDays);

        RefreshResponse response = RefreshResponse.builder()
                .status("SUCCESS")
                .processedSkills(rows.size())
                .refreshedAt(snapshotTime)
                .build();

        refreshCoordinator.markSuccess(response);
        return response;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private RuntimeException notFoundOrUnavailable() {
        if (refreshCoordinator.isDegraded()) {
            throw new DemandServiceUnavailableException(
                    "No skill gap analytics available and demand-service is unavailable: "
                            + refreshCoordinator.getDegradedReason());
        }
        throw new SkillGapDataNotFoundException(
                "No skill gap analytics available. Please run refresh.");
    }

    private SkillGapRowDto mapToRow(SkillGapAnalytics entity) {
        return SkillGapRowDto.builder()
                .skillName(entity.getSkillName())
                .demandCount(entity.getDemandCount())
                .benchCount(entity.getBenchCount())
                .gapScore(entity.getGapScore())
                .gapLevel(entity.getGapLevel().name())
                .trendDirection(
                        entity.getTrendDirection() != null
                                ? entity.getTrendDirection().name()
                                : "STABLE")
                .build();
    }

    private Map<String, Integer> buildDemandSkillMap(List<DemandResponse> demands) {
        Map<String, Integer> result = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        for (DemandResponse demand : demands) {
            if (demand == null || demand.getRequiredSkills() == null) continue;

            int headcount = resolveDemandHeadcount(demand);

            for (String skill : demand.getRequiredSkills()) {
                String normalized = normalizeSkillName(skill);
                if (normalized == null) continue;
                result.merge(normalized, headcount, Integer::sum);
            }
        }
        return result;
    }

    private int resolveDemandHeadcount(DemandResponse demand) {
        Integer headcount = demand.getHeadcount();
        return (headcount == null || headcount < 1) ? 1 : headcount;
    }

    private Map<String, Integer> buildBenchSkillMap(List<EngineerResponse> engineers) {
        Map<String, Integer> result = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        for (EngineerResponse engineer : engineers) {
            if (engineer == null || engineer.getSkills() == null) continue;

            for (String skill : engineer.getSkills()) {
                String normalized = normalizeSkillName(skill);
                if (normalized == null) continue;
                result.merge(normalized, 1, Integer::sum);
            }
        }
        return result;
    }

    private String normalizeSkillName(String skill) {
        if (skill == null) return null;
        String normalized = skill.trim().replaceAll("\\s+", " ");
        return normalized.isEmpty() ? null : normalized;
    }

    private GapLevel determineGapLevel(int gapScore) {
        if (gapScore >= 10) return GapLevel.CRITICAL;
        if (gapScore >= 5)  return GapLevel.HIGH;
        if (gapScore >= 2)  return GapLevel.MEDIUM;
        return GapLevel.LOW;
    }

    private TrendDirection determineTrend(String skill, int currentGap,
                                          Map<String, Integer> previousGapScores) {
        Integer previousGap = previousGapScores.get(skill);
        if (previousGap == null) return TrendDirection.STABLE;
        if (currentGap > previousGap) return TrendDirection.DOWN;
        if (currentGap < previousGap) return TrendDirection.UP;
        return TrendDirection.STABLE;
    }
}
