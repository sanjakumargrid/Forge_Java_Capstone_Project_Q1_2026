package com.talentgrid.demand.domain.statemachine;

import com.talentgrid.demand.domain.enums.DemandStatus;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Legal demand lifecycle transitions (TalentGrid workflow v2).
 */
@Component
public class DemandStateMachine {

    private static final Map<DemandStatus, Set<DemandStatus>> TRANSITIONS;

    static {
        TRANSITIONS = new EnumMap<>(DemandStatus.class);

        TRANSITIONS.put(DemandStatus.DRAFT,
                EnumSet.of(DemandStatus.PENDING_APPROVAL, DemandStatus.APPROVED));

        TRANSITIONS.put(DemandStatus.PENDING_APPROVAL,
                EnumSet.of(DemandStatus.APPROVED, DemandStatus.CLOSED));

        TRANSITIONS.put(DemandStatus.APPROVED,
                EnumSet.of(DemandStatus.INTERNAL_SEARCH, DemandStatus.OPEN_EXTERNAL));

        TRANSITIONS.put(DemandStatus.INTERNAL_SEARCH,
                EnumSet.of(DemandStatus.OPEN_EXTERNAL, DemandStatus.FILLED, DemandStatus.ON_HOLD));

        TRANSITIONS.put(DemandStatus.OPEN_EXTERNAL,
                EnumSet.of(DemandStatus.FILLED, DemandStatus.ON_HOLD));

        TRANSITIONS.put(DemandStatus.FILLED,
                EnumSet.of(DemandStatus.CLOSED));

        TRANSITIONS.put(DemandStatus.ON_HOLD,
                EnumSet.of(DemandStatus.INTERNAL_SEARCH, DemandStatus.OPEN_EXTERNAL, DemandStatus.CLOSED));

        TRANSITIONS.put(DemandStatus.CLOSED, EnumSet.noneOf(DemandStatus.class));
    }

    public boolean canTransition(DemandStatus current, DemandStatus next) {
        Set<DemandStatus> allowed = TRANSITIONS.get(current);
        return allowed != null && allowed.contains(next);
    }

    public Set<DemandStatus> getAllowedTransitions(DemandStatus current) {
        return TRANSITIONS.getOrDefault(current, EnumSet.noneOf(DemandStatus.class));
    }
}
