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

        Scope demandView =
                scopeRepository.findByName("DEMAND_VIEW")
                        .orElseGet(() ->
                                scopeRepository.save(
                                        Scope.builder()
                                                .name("DEMAND_VIEW")
                                                .description("View demands")
                                                .build()
                                )
                        );

        Scope demandCreate =
                scopeRepository.findByName("DEMAND_CREATE")
                        .orElseGet(() ->
                                scopeRepository.save(
                                        Scope.builder()
                                                .name("DEMAND_CREATE")
                                                .description("Create demands")
                                                .build()
                                )
                        );

        Scope demandUpdate =
                scopeRepository.findByName("DEMAND_UPDATE")
                        .orElseGet(() ->
                                scopeRepository.save(
                                        Scope.builder()
                                                .name("DEMAND_UPDATE")
                                                .description("Update demands")
                                                .build()
                                )
                        );

        Scope demandDelete =
                scopeRepository.findByName("DEMAND_DELETE")
                        .orElseGet(() ->
                                scopeRepository.save(
                                        Scope.builder()
                                                .name("DEMAND_DELETE")
                                                .description("Delete demands")
                                                .build()
                                )
                        );

        Scope demandApprove =
                scopeRepository.findByName("DEMAND_APPROVE")
                        .orElseGet(() ->
                                scopeRepository.save(
                                        Scope.builder()
                                                .name("DEMAND_APPROVE")
                                                .description("Approve demands")
                                                .build()
                                )
                        );

        Scope demandStatusTransition =
                scopeRepository.findByName("DEMAND_STATUS_TRANSITION")
                        .orElseGet(() ->
                                scopeRepository.save(
                                        Scope.builder()
                                                .name("DEMAND_STATUS_TRANSITION")
                                                .description("Demand Status Transitions")
                                                .build()
                                )
                        );

        Scope demandPipelineView =
                scopeRepository.findByName("DEMAND_PIPELINE_VIEW")
                        .orElseGet(() ->
                                scopeRepository.save(
                                        Scope.builder()
                                                .name("DEMAND_PIPELINE_VIEW")
                                                .description("View demands pipeline")
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
                            role.getScopes().add(demandCreate);
                            role.getScopes().add(demandUpdate);
                            role.getScopes().add(demandDelete);
                            role.getScopes().add(demandView);
                            role.getScopes().add(demandApprove);
                            role.getScopes().add(demandStatusTransition);
                            role.getScopes().add(demandPipelineView);

                            return roleRepository.save(role);
                        });

        // =========================================
        // CREATE ADMIN USER
        // =========================================

        if (!userRepository.existsByEmail(
                "<User-Name>@griddynamics.com"
        )) {

            User admin = User.builder()
                    .username("<User-Name>")
                    .email("<User-Name>@griddynamics.com")
                    .password(
                            passwordEncoder.encode("<Password>")
                    )
                    .enabled(true)
                    .roles(Set.of(adminRole))
                    .build();

            userRepository.save(admin);

            log.info("Default admin user created.");
        }
    }
}