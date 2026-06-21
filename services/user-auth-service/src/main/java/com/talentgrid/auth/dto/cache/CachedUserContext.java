package com.talentgrid.auth.dto.cache;

import lombok.*;

import java.io.Serializable;
import java.util.Set;

/**
 * Data transfer object representing the user's authorization state stored in Redis.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Store the user's current roles and scopes in a fast-access cache</li>
 *   <li>Store the user's {@code authVersion} for JWT invalidation checks</li>
 *   <li>Prevent the need to query the database on every authenticated request</li>
 * </ul>
 *
 * <p>This object is serialized to JSON and stored in Redis upon login.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CachedUserContext implements Serializable {

    /** The unique database identifier of the user. */
    private Long userId;

    /** The email address of the user. */
    private String email;

    /** Whether the user account is currently enabled. */
    private Boolean enabled;

    /** The current authorization version. If a JWT has an older version, it is rejected. */
    private Long authVersion;

    /** The role names assigned to the user. */
    private Set<String> roles;

    /** The scope (permission) names granted to the user. */
    private Set<String> scopes;
}