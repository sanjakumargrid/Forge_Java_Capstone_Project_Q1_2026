package com.talentgrid.workforce.skillgapheatmap.service;

import com.talentgrid.workforce.skillgapheatmap.dto.RefreshResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Serialises refresh execution and debounces bursty Kafka triggers so only one
 * snapshot is calculated within the configured window.
 */
@Slf4j
@Component
public class SkillGapRefreshCoordinator {

    private final ReentrantLock refreshLock = new ReentrantLock();

    @Value("${skill-gap.refresh-debounce-seconds:30}")
    private int debounceSeconds;

    private volatile LocalDateTime lastSuccessfulRefreshAt;
    private volatile RefreshResponse lastSuccessfulRefresh;
    private volatile boolean degraded;
    private volatile String degradedReason;
    private volatile LocalDateTime degradedSince;

    public boolean tryAcquireLock() {
        return refreshLock.tryLock();
    }

    public void releaseLock() {
        if (refreshLock.isHeldByCurrentThread()) {
            refreshLock.unlock();
        }
    }

    public RefreshResponse debouncedResponse() {
        RefreshResponse last = lastSuccessfulRefresh;
        if (last == null) {
            return null;
        }
        log.info("[SKILL-GAP] Refresh debounced — last successful refresh was {}s ago.",
                secondsSince(last.getRefreshedAt()));
        return RefreshResponse.builder()
                .status("DEBOUNCED")
                .processedSkills(last.getProcessedSkills())
                .refreshedAt(last.getRefreshedAt())
                .build();
    }

    public boolean isDebounced() {
        if (lastSuccessfulRefreshAt == null) {
            return false;
        }
        return secondsSince(lastSuccessfulRefreshAt) < debounceSeconds;
    }

    public RefreshResponse skippedInProgressResponse() {
        RefreshResponse last = lastSuccessfulRefresh;
        log.info("[SKILL-GAP] Refresh skipped — another refresh is already in progress.");
        return RefreshResponse.builder()
                .status("SKIPPED_IN_PROGRESS")
                .processedSkills(last != null ? last.getProcessedSkills() : 0)
                .refreshedAt(last != null ? last.getRefreshedAt() : null)
                .build();
    }

    public void markSuccess(RefreshResponse response) {
        lastSuccessfulRefresh = response;
        lastSuccessfulRefreshAt = response.getRefreshedAt();
        degraded = false;
        degradedReason = null;
        degradedSince = null;
    }

    public void markDegraded(String reason) {
        degraded = true;
        degradedReason = reason;
        degradedSince = LocalDateTime.now();
    }

    public boolean isDegraded() {
        return degraded;
    }

    public String getDegradedReason() {
        return degradedReason;
    }

    public LocalDateTime getDegradedSince() {
        return degradedSince;
    }

    private long secondsSince(LocalDateTime timestamp) {
        return Duration.between(timestamp, LocalDateTime.now()).getSeconds();
    }
}
