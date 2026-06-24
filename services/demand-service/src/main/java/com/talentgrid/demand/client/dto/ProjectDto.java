package com.talentgrid.demand.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lightweight DTO projection representing a Project from the User Auth Service.
 * Used exclusively for denormalizing the project name during demand creation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDto {
    private Long id;
    private Long accountId;
    private String name;
    private Long projectManagerId;
}
