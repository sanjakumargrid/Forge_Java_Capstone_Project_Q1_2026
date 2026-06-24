package com.talentgrid.workforce.rmgdashboard.dto;

import com.talentgrid.workforce.rmgdashboard.enums.DemandClosureReason;
import com.talentgrid.workforce.rmgdashboard.enums.DemandTransitionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DemandStatusTransitionRequest {
    private DemandTransitionStatus targetStatus;
    private DemandClosureReason closureReason;
    private String comments;
}
