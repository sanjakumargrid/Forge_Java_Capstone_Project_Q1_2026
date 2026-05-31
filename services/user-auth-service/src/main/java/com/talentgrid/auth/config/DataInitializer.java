package com.talentgrid.auth.config;

import com.talentgrid.auth.entity.Role;
import com.talentgrid.auth.entity.Scope;
import com.talentgrid.auth.entity.User;
import com.talentgrid.auth.repository.RoleRepository;
import com.talentgrid.auth.repository.ScopeRepository;
import com.talentgrid.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer
        implements CommandLineRunner {

    private final UserRepository userRepository;

    private final RoleRepository roleRepository;

    private final ScopeRepository scopeRepository;

    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {

        // =========================================
        // CREATE SCOPES
        // =========================================

        Scope userCreate =
                scopeRepository.findByName("USER_CREATE")
                        .orElseGet(() ->
                                scopeRepository.save(
                                        Scope.builder()
                                                .name("USER_CREATE")
                                                .description("Create users")
                                                .build()
                                )
                        );

        Scope userDelete =
                scopeRepository.findByName("USER_DELETE")
                        .orElseGet(() ->
                                scopeRepository.save(
                                        Scope.builder()
                                                .name("USER_DELETE")
                                                .description("Delete users")
                                                .build()
                                )
                        );

        Scope userView =
                scopeRepository.findByName("USER_VIEW")
                        .orElseGet(() ->
                                scopeRepository.save(
                                        Scope.builder()
                                                .name("USER_VIEW")
                                                .description("View users")
                                                .build()
                                )
                        );

        // =========================================
        // CREATE ADMIN ROLE
        // =========================================

        Role adminRole =
                roleRepository.findByName("ADMIN")
                        .orElseGet(() -> {

                            Role role = Role.builder()
                                    .name("ADMIN")
                                    .scopes(new HashSet<>())
                                    .build();

                            role.getScopes().add(userCreate);
                            role.getScopes().add(userDelete);
                            role.getScopes().add(userView);

                            return roleRepository.save(role);
                        });

        // =========================================
        // CREATE ADMIN USER
        // =========================================

        if (!userRepository.existsByEmail(
                "admin@griddynamics.com"
        )) {

            User admin = User.builder()
                    .username("Admin")
                    .email("admin@griddynamics.com")
                    .password(
                            passwordEncoder.encode("admin@123")
                    )
                    .enabled(true)
                    .roles(Set.of(adminRole))
                    .build();

            userRepository.save(admin);

            log.info("Default admin user created.");
        }
    }
}