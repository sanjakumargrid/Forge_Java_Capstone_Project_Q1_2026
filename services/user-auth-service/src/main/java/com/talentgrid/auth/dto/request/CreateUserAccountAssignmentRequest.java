package com.talentgrid.auth.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request body for creating a user-account assignment.
 */
@Getter
@Setter
@NoArgsConstructor
public class CreateUserAccountAssignmentRequest {

    @NotNull(message = "User id is required")
    private Long userId;

    @NotNull(message = "Account id is required")
    private Long accountId;

    /** Optional project scope for the assignment. */
    private Long projectId;

    /** Optional role name to assign to the user (e.g. PROJECT_MANAGER). */
    private String roleName;
}
