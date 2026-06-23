package com.talentgrid.kafka.events.auth;

import lombok.*;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthUserPayload {

    private Long userId;
    private Long authVersion;
    private Boolean enabled;
    private Set<String> roles;
    private Set<String> scopes;
}