package com.talentgrid.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Builder
@AllArgsConstructor
public class AdminUserResponse {

    private final Long id;
    private final String username;
    private final String email;
    private final String location;
    private final Boolean enabled;
    private final Boolean accountLocked;
    private final String slackId;
    private final Set<String> roles;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
}
