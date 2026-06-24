package com.talentgrid.workforce.rmganalyticsdashboard.dto;

import com.talentgrid.workforce.rmgdashboard.dto.DemandDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemandsRequiringNominationsResponse {

    @Builder.Default
    private List<DemandDto> demands = new ArrayList<>();
}
