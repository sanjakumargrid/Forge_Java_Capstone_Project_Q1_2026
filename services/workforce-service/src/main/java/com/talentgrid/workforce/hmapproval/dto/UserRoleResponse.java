package com.talentgrid.workforce.hmapproval.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserRoleResponse {

    private Long id;
    private String username;
    private String email;
    private String location;
    private Boolean enabled;
    private Set<String> roles;
}
