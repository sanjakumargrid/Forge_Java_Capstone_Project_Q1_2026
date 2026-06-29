package com.talentgrid.auth.mapper;

import com.talentgrid.auth.dto.response.AdminUserResponse;
import com.talentgrid.auth.entity.Project;
import com.talentgrid.auth.entity.Role;
import com.talentgrid.auth.entity.User;
import com.talentgrid.auth.repository.ProjectRepository;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * Mapper for converting User entities to various DTOs.
 */
@Component
public class UserMapper {

    private final ProjectRepository projectRepository;

    public UserMapper(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    /**
     * Maps a User entity to an AdminUserResponse DTO.
     *
     * @param user the User entity
     * @return the mapped AdminUserResponse
     */
    public AdminUserResponse toAdminUserResponse(User user) {
        if (user == null) {
            return null;
        }

        String projectName = null;
        if (user.getProjectId() != null) {
            projectName = projectRepository.findById(user.getProjectId())
                    .map(Project::getName)
                    .orElse(null);
        }

        return AdminUserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .location(user.getLocation())
                .enabled(user.getEnabled())
                .accountLocked(user.getAccountLocked())
                .slackId(user.getSlackId())
                .roles(user.getRoles().stream()
                        .map(Role::getName)
                        .collect(Collectors.toSet()))
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .projectId(user.getProjectId())
                .projectName(projectName)
                .build();
    }
}
