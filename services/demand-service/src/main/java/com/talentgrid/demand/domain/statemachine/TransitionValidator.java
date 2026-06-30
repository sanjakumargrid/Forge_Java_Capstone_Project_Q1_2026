package com.talentgrid.demand.domain.statemachine;

import com.talentgrid.demand.domain.entity.Demand;
import com.talentgrid.demand.domain.enums.ClosureReason;
import com.talentgrid.demand.domain.enums.DemandStatus;
import com.talentgrid.demand.exception.IllegalDemandTransitionException;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.Set;

/**
 * Validates demand transitions: matrix, internal-search gate, closure reasons, ON_HOLD resume.
 */
@Component
public class TransitionValidator {

    private static final Set<ClosureReason> EARLY_OPEN_EXTERNAL_REASONS = EnumSet.of(
            ClosureReason.NO_INTERNAL_MATCH,
            ClosureReason.HM_REJECTED_NOMINATION);

    private final DemandStateMachine stateMachine;

    public TransitionValidator(DemandStateMachine stateMachine) {
        this.stateMachine = stateMachine;
    }

    public void validate(Demand demand, DemandStatus targetStatus, ClosureReason closureReason) {
        DemandStatus currentStatus = demand.getStatus();

        if (!stateMachine.canTransition(currentStatus, targetStatus)) {
            Set<DemandStatus> allowed = stateMachine.getAllowedTransitions(currentStatus);
            throw new IllegalDemandTransitionException(
                    String.format("Transition from %s to %s is not allowed. Allowed targets: %s",
                            currentStatus, targetStatus, allowed));
        }

        if (currentStatus == DemandStatus.INTERNAL_SEARCH
                && targetStatus == DemandStatus.OPEN_EXTERNAL) {
            validateInternalSearchToOpenExternal(demand, closureReason);
        }

        validateClosureReason(currentStatus, targetStatus, closureReason);

        if (currentStatus == DemandStatus.ON_HOLD) {
            validateOnHoldResume(demand, targetStatus);
        }
    }

    private void validateInternalSearchToOpenExternal(Demand demand, ClosureReason closureReason) {
        if (closureReason != null && EARLY_OPEN_EXTERNAL_REASONS.contains(closureReason)) {
            return;
        }
        validateRmgFiveDayGate(demand);
    }

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
                            "Internal search gate: OPEN_EXTERNAL requires 5 business days since search started "
                                    + "unless closureReason is NO_INTERNAL_MATCH or HM_REJECTED_NOMINATION. "
                                    + "Only %d business day(s) have elapsed.",
                            businessDaysElapsed));
        }
    }

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

    private void validateClosureReason(DemandStatus fromStatus, DemandStatus targetStatus,
                                       ClosureReason closureReason) {
        if (targetStatus == DemandStatus.FILLED) {
            if (closureReason != ClosureReason.FILLED) {
                throw new IllegalDemandTransitionException(
                        String.format("Transition to FILLED requires closureReason FILLED, got %s.",
                                closureReason));
            }
            return;
        }

        if (targetStatus == DemandStatus.ON_HOLD) {
            requireExact(closureReason, ClosureReason.ON_HOLD, DemandStatus.ON_HOLD);
            return;
        }

        if (targetStatus == DemandStatus.CLOSED) {
            validateClosedReason(fromStatus, closureReason);
            return;
        }

    }

    private void validateClosedReason(DemandStatus fromStatus, ClosureReason closureReason) {
        if (closureReason == null) {
            throw new IllegalDemandTransitionException(
                    "Transition to CLOSED requires a closureReason.");
        }
        switch (fromStatus) {
            case FILLED -> {
                if (closureReason != ClosureReason.AUTO_CLOSED_AFTER_FILL) {
                    throw new IllegalDemandTransitionException(
                            "Transition from FILLED to CLOSED requires closureReason=AUTO_CLOSED_AFTER_FILL.");
                }
            }
            case PENDING_APPROVAL -> {
                if (closureReason != ClosureReason.PM_REJECTED
                        && closureReason != ClosureReason.SLA_APPROVAL_BREACH
                        && closureReason != ClosureReason.HM_CLOSED) {
                    throw new IllegalDemandTransitionException(
                            "Transition from PENDING_APPROVAL to CLOSED requires closureReason=PM_REJECTED, SLA_APPROVAL_BREACH, or HM_CLOSED.");
                }
            }
            case ON_HOLD -> {
                if (closureReason != ClosureReason.RM_CLOSED_ON_HOLD) {
                    throw new IllegalDemandTransitionException(
                            "Transition from ON_HOLD to CLOSED requires closureReason=RM_CLOSED_ON_HOLD.");
                }
            }
            default -> throw new IllegalDemandTransitionException(
                    "Unsupported transition to CLOSED from status: " + fromStatus);
        }
    }

    private void requireExact(ClosureReason closureReason, ClosureReason required, DemandStatus targetStatus) {
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

    private void validateOnHoldResume(Demand demand, DemandStatus targetStatus) {
        if (targetStatus != DemandStatus.INTERNAL_SEARCH
                && targetStatus != DemandStatus.OPEN_EXTERNAL) {
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
                            "ON_HOLD resume guard: demand was in %s before being held. It can only resume to %s, not %s.",
                            previous, previous, targetStatus));
        }
    }
}
