package com.talentgrid.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "location", length = 150)
    private String location;

    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @Column(name = "failed_attempts")
    @Builder.Default
    private Integer failedAttempts = 0;

    @Column(name = "account_locked")
    @Builder.Default
    private Boolean accountLocked = false;

    @Column(name = "slack_id", length = 50)
    private String slackId;

    @Column(name = "lock_time")
    private LocalDateTime lockTime;

    @Column(name = "is_interviewer_eligible")
    @Builder.Default
    private Boolean isInterviewerEligible = false;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    /**
     * Incremented when permissions change; embedded in JWT for invalidation.
     * DB default avoids failed {@code ddl-auto=update} when adding NOT NULL to a non-empty {@code users} table.
     */
    @Column(name = "auth_version", nullable = false)
    @ColumnDefault("1")
    @Builder.Default
    private Long authVersion = 1L;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.authVersion == null) {
            this.authVersion = 1L;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}