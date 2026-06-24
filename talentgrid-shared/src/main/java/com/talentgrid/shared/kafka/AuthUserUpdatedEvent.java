package com.talentgrid.shared.kafka;

import lombok.*;

import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthUserUpdatedEvent {

    private Long userId;
    private Long authVersion;
    private Boolean enabled;
    private Set<String> roles;
    private Set<String> scopes;
}