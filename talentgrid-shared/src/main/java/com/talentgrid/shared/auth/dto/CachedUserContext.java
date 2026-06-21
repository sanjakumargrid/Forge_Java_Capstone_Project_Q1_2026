package com.talentgrid.shared.auth.dto;

import lombok.*;
import java.io.Serializable;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CachedUserContext implements Serializable {

    private Long userId;
    private String email;
    private Boolean enabled;
    private Long authVersion;
    private Set<String> roles;
    private Set<String> scopes;
}