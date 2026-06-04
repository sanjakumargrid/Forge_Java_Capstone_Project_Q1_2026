package com.talentgrid.demand.domain.statemachine;

import com.talentgrid.demand.domain.entity.Demand;
import com.talentgrid.demand.domain.enums.ClosureReason;
import com.talentgrid.demand.domain.enums.DemandStatus;
import com.talentgrid.demand.exception.IllegalDemandTransitionException;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * Enforces all business rules governing demand state transitions.
 *
 * <p>Three categories of validation are applied in order:
 * <ol>
 *   <li><b>Matrix check</b> — transition must be present in {@link DemandStateMachine}.</li>
 *   <li><b>RMG 5-business-day gate</b> — {@code INTERNAL_SEARCH → OPEN_EXTERNAL} is blocked
 *       until at least 5 business days have elapsed since {@code searchStartAt}.</li>
 *   <li><b>Closure reason enforcement</b> — the {@code closureReason} supplied must match
 *       the target state.</li>
 *   <li><b>ON_HOLD resume guard</b> — when resuming from {@code ON_HOLD}, the target must
 *       equal the demand's {@code previousStatus}.</li>
 * </ol>
 *
 * <p>All violations throw {@link IllegalDemandTransitionException} which maps to HTTP 400.
 */
@Component
public class TransitionValidator {

    /**
     * Maps each target {@link DemandStatus} to the only {@link ClosureReason} that is
     * valid for that target.  States without a required reason are not present in the map.
     */
    private static final Map<DemandStatus, ClosureReason> REQUIRED_REASON_BY_TARGET;

    static {
        REQUIRED_REASON_BY_TARGET = new EnumMap<>(DemandStatus.class);
        REQUIRED_REASON_BY_TARGET.put(DemandStatus.FILLED_INTERNAL, ClosureReason.FILLED_INTERNAL);
        REQUIRED_REASON_BY_TARGET.put(DemandStatus.FILLED_EXTERNAL, ClosureReason.FILLED_EXTERNAL);
        REQUIRED_REASON_BY_TARGET.put(DemandStatus.CANCELLED,       ClosureReason.CANCELLED);
        REQUIRED_REASON_BY_TARGET.put(DemandStatus.ON_HOLD,         ClosureReason.ON_HOLD);
        REQUIRED_REASON_BY_TARGET.put(DemandStatus.DUPLICATE,       ClosureReason.DUPLICATE);
    }

    private final DemandStateMachine stateMachine;

    public TransitionValidator(DemandStateMachine stateMachine) {
        this.stateMachine = stateMachine;
    }

    /**
     * Validates a requested status transition against all business rules.
     *
     * @param demand        the demand being transitioned (must not be {@code null})
     * @param targetStatus  the desired next status
     * @param closureReason the closure/transition reason supplied by the caller (may be {@code null})
     * @throws IllegalDemandTransitionException if any rule is violated
     */
    public void validate(Demand demand, DemandStatus targetStatus, ClosureReason closureReason) {
        DemandStatus currentStatus = demand.getStatus();

        // ── 1. Matrix check ─────────────────────────────────────────────────────
        if (!stateMachine.canTransition(currentStatus, targetStatus)) {
            Set<DemandStatus> allowed = stateMachine.getAllowedTransitions(currentStatus);
            throw new IllegalDemandTransitionException(
                    String.format("Transition from %s to %s is not allowed. Allowed targets: %s",
                            currentStatus, targetStatus, allowed));
        }

        // ── 2. RMG 5-business-day internal-first gate ────────────────────────────
        if (currentStatus == DemandStatus.INTERNAL_SEARCH
                && targetStatus == DemandStatus.OPEN_EXTERNAL) {
            validateRmgFiveDayGate(demand);
        }

        // ── 3. Closure reason enforcement ────────────────────────────────────────
        validateClosureReason(targetStatus, closureReason);

        // ── 4. ON_HOLD resume guard ──────────────────────────────────────────────
        if (currentStatus == DemandStatus.ON_HOLD) {
            validateOnHoldResume(demand, targetStatus);
        }
    }

    /**
     * Convenience overload that accepts a {@code DemandTransition} record.
     * Delegates to {@link #validate(Demand, DemandStatus, ClosureReason)}.
     */
    public void validate(DemandTransition transition) {
        // This overload is kept for backward compatibility.
        // Full validation requires the Demand entity; use the primary overload instead.
        if (!stateMachine.canTransition(transition.getFrom(), transition.getTo())) {
            throw new IllegalDemandTransitionException(
                    String.format("Transition from %s to %s is not allowed.",
                            transition.getFrom(), transition.getTo()));
        }
    }

    // ─── Private helpers ────────────────────────────────────────────────────────

    /**
     * Blocks {@code INTERNAL_SEARCH → OPEN_EXTERNAL} if fewer than 5 business days
     * have elapsed since {@code searchStartAt}.  Weekends are excluded from the count.
     */
    private void validateRmgFiveDayGate(Demand demand) {
        OffsetDateTime searchStartAt = demand.getSearchStartAt();
        if (searchStartAt == null) {
            throw new IllegalDemandTransitionException(
                    "Cannot transition to OPEN_EXTERNAL: searchStartAt is not set.");
        }

        int businessDaysElapsed = countBusinessDays(searchStartAt.toLocalDate(), LocalDate.now());
        if (businessDaysElapsed < 5) {
            throw new IllegalDemandTransitionException(
                    String.format(
                            "RMG internal-first gate: OPEN_EXTERNAL transition requires 5 business days " +
                            "since internal search started. Only %d business day(s) have elapsed.",
                            businessDaysElapsed));
        }
    }

    /**
     * Counts the number of business days (Mon–Fri) between {@code startInclusive}
     * and {@code endExclusive} (exclusive of the end date).
     */
    private int countBusinessDays(LocalDate start, LocalDate end) {
        int count = 0;
        LocalDate current = start;
        while (current.isBefore(end)) {
            DayOfWeek dow = current.getDayOfWeek();
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) {
                count++;
            }
            current = current.plusDays(1);
        }
        return count;
    }

    /**
     * Validates that the provided {@code closureReason} matches the expected reason
     * for the given {@code targetStatus}.  If the target state requires a specific reason
     * and none (or a mismatched one) is supplied, an exception is thrown.
     */
    private void validateClosureReason(DemandStatus targetStatus, ClosureReason closureReason) {
        ClosureReason required = REQUIRED_REASON_BY_TARGET.get(targetStatus);
        if (required == null) {
            // Target state does not require a reason — any value (including null) is fine.
            return;
        }
        if (closureReason == null) {
            throw new IllegalDemandTransitionException(
                    String.format("Transition to %s requires closureReason=%s, but none was provided.",
                            targetStatus, required));
        }
        if (closureReason != required) {
            throw new IllegalDemandTransitionException(
                    String.format("Transition to %s requires closureReason=%s, but received %s.",
                            targetStatus, required, closureReason));
        }
    }

    /**
     * When resuming from {@code ON_HOLD}, ensures the target status matches
     * the demand's {@code previousStatus} to prevent illegal state jumps.
     */
    private void validateOnHoldResume(Demand demand, DemandStatus targetStatus) {
        // Resuming = transitioning back to an active search state
        if (targetStatus != DemandStatus.INTERNAL_SEARCH
                && targetStatus != DemandStatus.OPEN_EXTERNAL) {
            // Not a resume — cancel/close from ON_HOLD requires no extra guard.
            return;
        }

        DemandStatus previous = demand.getPreviousStatus();
        if (previous == null) {
            throw new IllegalDemandTransitionException(
                    "Cannot resume from ON_HOLD: previousStatus is not recorded.");
        }
        if (targetStatus != previous) {
            throw new IllegalDemandTransitionException(
                    String.format(
                            "ON_HOLD resume guard: demand was in %s before being held. " +
                            "It can only resume to %s, not %s.",
                            previous, previous, targetStatus));
        }
    }
}
