package com.talentgrid.demand.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Project summary from user-auth for PM-scoped demand queries.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManagedProjectDto {

    private Long id;
    private String name;
    private Long accountId;
    private Long projectManagerId;
}
