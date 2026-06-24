package com.talentgrid.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

/**
 * JPA entity for application roles.
 *
 * <p>Supported role names: {@code ADMIN}, {@code HIRING_MANAGER}, {@code PORTFOLIO_MANAGER},
 * {@code RESOURCE_MANAGER}, {@code RECRUITER}, {@code TA_MANAGER}, {@code EMPLOYEE}.
 */
@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    private static final Set<String> ALLOWED_NAMES = Set.of(
            "ADMIN",
            "HIRING_MANAGER",
            "PORTFOLIO_MANAGER",
            "RESOURCE_MANAGER",
            "RECRUITER",
            "TA_MANAGER",
            "EMPLOYEE"
    );

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "role_scopes",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "scope_id")
    )
    private Set<Scope> scopes = new HashSet<>();

    /**
     * Validates that {@code roleName} is one of the supported application roles.
     *
     * @param roleName role name from an API request
     * @throws IllegalArgumentException when the role is not allowed
     */
    public static void requireAllowedName(String roleName) {
        if (roleName == null || !ALLOWED_NAMES.contains(roleName.toUpperCase())) {
            throw new IllegalArgumentException(
                    "Invalid role: " + roleName + ". Allowed roles: " + ALLOWED_NAMES);
        }
    }
}
