package com.talentgrid.auth.mapper;

import com.talentgrid.auth.dto.response.ProjectResponse;
import com.talentgrid.auth.entity.Project;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting {@link Project} entities to response DTOs.
 */
@Component
public class ProjectMapper {

    /**
     * Maps a project entity to its API response representation.
     *
     * @param project the persisted project
     * @return mapped response or {@code null} when the entity is {@code null}
     */
    public ProjectResponse toResponse(Project project) {
        if (project == null) {
            return null;
        }

        return ProjectResponse.builder()
                .id(project.getId())
                .accountId(project.getAccount() != null ? project.getAccount().getId() : null)
                .name(project.getName())
                .projectManagerId(project.getProjectManagerId())
                .build();
    }
}
