package com.talentgrid.auth.mapper;

import com.talentgrid.auth.dto.response.UserAccountAssignmentResponse;
import com.talentgrid.auth.entity.Role;
import com.talentgrid.auth.entity.UserAccountAssignment;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting {@link UserAccountAssignment} entities to response DTOs.
 */
@Component
public class UserAccountAssignmentMapper {

    /**
     * Maps an assignment entity to its API response representation.
     *
     * @param assignment the persisted assignment
     * @return mapped response or {@code null} when the entity is {@code null}
     */
    public UserAccountAssignmentResponse toResponse(UserAccountAssignment assignment) {
        if (assignment == null) {
            return null;
        }

        String roleName = null;
        if (assignment.getUser() != null && assignment.getUser().getRoles() != null) {
            roleName = assignment.getUser().getRoles().stream()
                    .map(Role::getName)
                    .findFirst()
                    .orElse(null);
        }

        return UserAccountAssignmentResponse.builder()
                .id(assignment.getId())
                .userId(assignment.getUser() != null ? assignment.getUser().getId() : null)
                .accountId(assignment.getAccount() != null ? assignment.getAccount().getId() : null)
                .projectId(assignment.getProject() != null ? assignment.getProject().getId() : null)
                .roleName(roleName)
                .createdAt(assignment.getCreatedAt())
                .build();
    }
}
