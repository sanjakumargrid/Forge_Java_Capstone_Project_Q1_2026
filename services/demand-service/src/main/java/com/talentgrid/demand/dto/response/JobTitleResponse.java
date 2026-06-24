package com.talentgrid.demand.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobTitleResponse {
    private Long jobTitleId;
    private String titleName;
}
