package com.talentgrid.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * User-account assignment response projection.
 */
@Getter
@Builder
@AllArgsConstructor
public class UserAccountAssignmentResponse {

    private final Long id;
    private final Long userId;
    private final Long accountId;
    private final Long projectId;
    private final String roleName;
    private final LocalDateTime createdAt;
}
