package com.talentgrid.demand.domain.statemachine;

import com.talentgrid.demand.domain.enums.DemandStatus;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Immutable state machine encoding all legal demand lifecycle transitions.
 *
 * <p>Implements the full 12-state transition matrix defined in the TalentGrid
 * demand workflow specification. Any transition not present in the matrix
 * is considered illegal and must result in an HTTP 400 response.
 *
 * <pre>
 * Transition matrix:
 *   DRAFT              → PENDING_APPROVAL, CANCELLED
 *   PENDING_APPROVAL   → APPROVED, DRAFT, DUPLICATE, ON_HOLD, CANCELLED
 *   APPROVED           → INTERNAL_SEARCH
 *   INTERNAL_SEARCH    → FILLED_INTERNAL, FILLED_PARTIALLY, OPEN_EXTERNAL, ON_HOLD, CANCELLED
 *   FILLED_PARTIALLY   → OPEN_EXTERNAL, CLOSED
 *   OPEN_EXTERNAL      → FILLED_EXTERNAL, ON_HOLD, CANCELLED
 *   ON_HOLD            → INTERNAL_SEARCH, OPEN_EXTERNAL, CANCELLED, CLOSED
 *   FILLED_INTERNAL    → CLOSED
 *   FILLED_EXTERNAL    → CLOSED
 *   CANCELLED          → CLOSED
 *   DUPLICATE          → CLOSED
 *   CLOSED             → (terminal — no transitions)
 * </pre>
 */
@Component
public class DemandStateMachine {

    private static final Map<DemandStatus, Set<DemandStatus>> TRANSITIONS;

    static {
        TRANSITIONS = new EnumMap<>(DemandStatus.class);

        TRANSITIONS.put(DemandStatus.DRAFT,
                EnumSet.of(DemandStatus.PENDING_APPROVAL, DemandStatus.CANCELLED));

        TRANSITIONS.put(DemandStatus.PENDING_APPROVAL,
                EnumSet.of(DemandStatus.APPROVED, DemandStatus.DRAFT,
                        DemandStatus.DUPLICATE, DemandStatus.ON_HOLD, DemandStatus.CANCELLED));

        TRANSITIONS.put(DemandStatus.APPROVED,
                EnumSet.of(DemandStatus.INTERNAL_SEARCH));

        TRANSITIONS.put(DemandStatus.INTERNAL_SEARCH,
                EnumSet.of(DemandStatus.FILLED_INTERNAL, DemandStatus.FILLED_PARTIALLY,
                        DemandStatus.OPEN_EXTERNAL, DemandStatus.ON_HOLD, DemandStatus.CANCELLED));

        TRANSITIONS.put(DemandStatus.FILLED_PARTIALLY,
                EnumSet.of(DemandStatus.OPEN_EXTERNAL, DemandStatus.CLOSED));

        TRANSITIONS.put(DemandStatus.OPEN_EXTERNAL,
                EnumSet.of(DemandStatus.FILLED_EXTERNAL, DemandStatus.ON_HOLD, DemandStatus.CANCELLED));

        TRANSITIONS.put(DemandStatus.ON_HOLD,
                EnumSet.of(DemandStatus.INTERNAL_SEARCH, DemandStatus.OPEN_EXTERNAL,
                        DemandStatus.CANCELLED, DemandStatus.CLOSED));

        TRANSITIONS.put(DemandStatus.FILLED_INTERNAL,
                EnumSet.of(DemandStatus.CLOSED));

        TRANSITIONS.put(DemandStatus.FILLED_EXTERNAL,
                EnumSet.of(DemandStatus.CLOSED));

        TRANSITIONS.put(DemandStatus.CANCELLED,
                EnumSet.of(DemandStatus.CLOSED));

        TRANSITIONS.put(DemandStatus.DUPLICATE,
                EnumSet.of(DemandStatus.CLOSED));

        // CLOSED is terminal — no outgoing transitions
        TRANSITIONS.put(DemandStatus.CLOSED, EnumSet.noneOf(DemandStatus.class));
    }

    /**
     * Returns whether a transition from {@code current} to {@code next} is legal
     * according to the state machine matrix.
     *
     * @param current the demand's current status
     * @param next    the requested target status
     * @return {@code true} if the transition is in the matrix; {@code false} otherwise
     */
    public boolean canTransition(DemandStatus current, DemandStatus next) {
        Set<DemandStatus> allowed = TRANSITIONS.get(current);
        return allowed != null && allowed.contains(next);
    }

    /**
     * Returns the set of all legal target states reachable from the given status.
     *
     * @param current the current demand status
     * @return an unmodifiable set of allowed next states (may be empty for terminal states)
     */
    public Set<DemandStatus> getAllowedTransitions(DemandStatus current) {
        return TRANSITIONS.getOrDefault(current, EnumSet.noneOf(DemandStatus.class));
    }
}
