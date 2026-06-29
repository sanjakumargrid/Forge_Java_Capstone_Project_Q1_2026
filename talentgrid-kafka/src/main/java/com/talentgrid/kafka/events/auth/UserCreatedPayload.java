package com.talentgrid.kafka.events.auth;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCreatedPayload {

    private Long userId;
    private String username;
    private String email;
    private String role;
    private String location;
    private String projectName;
    private String source; // "SELF_REGISTER" or "ADMIN_CREATED"
}