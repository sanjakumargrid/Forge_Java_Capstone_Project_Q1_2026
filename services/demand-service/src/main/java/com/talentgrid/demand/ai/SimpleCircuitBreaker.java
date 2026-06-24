package com.talentgrid.demand.ai;

import java.time.Duration;
import java.time.Instant;

public class SimpleCircuitBreaker {

    private final int failureThreshold;
    private final Duration openDuration;
    private int consecutiveFailures;
    private Instant openedAt;

    public SimpleCircuitBreaker(int failureThreshold, Duration openDuration) {
        this.failureThreshold = failureThreshold;
        this.openDuration = openDuration;
    }

    public boolean isOpen() {
        if (openedAt == null) {
            return false;
        }
        if (Instant.now().isAfter(openedAt.plus(openDuration))) {
            openedAt = null;
            consecutiveFailures = 0;
            return false;
        }
        return true;
    }

    public void recordSuccess() {
        consecutiveFailures = 0;
        openedAt = null;
    }

    public void recordFailure() {
        consecutiveFailures++;
        if (consecutiveFailures >= failureThreshold) {
            openedAt = Instant.now();
        }
    }
}
