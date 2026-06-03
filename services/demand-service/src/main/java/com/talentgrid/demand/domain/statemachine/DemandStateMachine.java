package com.talentgrid.demand.domain.statemachine;

import com.talentgrid.demand.domain.enums.DemandStatus;
import org.springframework.stereotype.Component;

@Component
public class DemandStateMachine {
    public boolean canTransition(DemandStatus current, DemandStatus next) {
        return true;
    }
}
