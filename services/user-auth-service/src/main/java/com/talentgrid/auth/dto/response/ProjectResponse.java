package com.talentgrid.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Project response projection compatible with downstream Feign clients.
 */
@Getter
@Builder
@AllArgsConstructor
public class ProjectResponse {

    private final Long id;
    private final Long accountId;
    private final String name;
    private final Long projectManagerId;
}
