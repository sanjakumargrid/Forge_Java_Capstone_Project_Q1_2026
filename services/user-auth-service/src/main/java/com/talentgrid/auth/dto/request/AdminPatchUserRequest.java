package com.talentgrid.auth.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AdminPatchUserRequest {

    private String username;
    private String location;
    private String slackId;
    private String role;
    private Long projectId;
}
