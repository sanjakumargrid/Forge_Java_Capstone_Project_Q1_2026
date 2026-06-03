package com.talentgrid.demand.domain.statemachine;

import com.talentgrid.demand.domain.enums.DemandStatus;

public class DemandTransition {
    private final DemandStatus from;
    private final DemandStatus to;

    public DemandTransition(DemandStatus from, DemandStatus to) {
        this.from = from;
        this.to = to;
    }

    public DemandStatus getFrom() { return from; }
    public DemandStatus getTo() { return to; }
}
