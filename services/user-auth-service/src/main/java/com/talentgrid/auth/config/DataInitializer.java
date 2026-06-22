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

/**
 * Seeds scopes and roles for local/dev. Align scope names with
 * {@code talentgrid-api-gateway-service} {@code rbac-rules.yml} and demand-service {@code @PreAuthorize}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ScopeRepository scopeRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        Scope userCreate = upsertScope("USER_CREATE", "Create users");
        Scope userDelete = upsertScope("USER_DELETE", "Delete users");
        Scope userView = upsertScope("USER_VIEW", "View users");

        Scope demandView = upsertScope("DEMAND_VIEW", "View demands");
        Scope demandCreate = upsertScope("DEMAND_CREATE", "Create demands");
        Scope demandUpdate = upsertScope("DEMAND_UPDATE", "Update demands");
        Scope demandDelete = upsertScope("DEMAND_DELETE", "Delete demands");
        Scope demandStatusTransition = upsertScope("DEMAND_STATUS_TRANSITION", "Demand status transitions");
        Scope demandPipelineView = upsertScope("DEMAND_PIPELINE_VIEW", "View demand pipeline");
        Scope demandPmApprove = upsertScope("DEMAND_PM_APPROVE", "Project manager demand actions");
        Scope demandSubmit = upsertScope("DEMAND_SUBMIT", "Submit demand from draft (HM path)");
        Scope demandNominate = upsertScope("DEMAND_NOMINATE", "Create internal nominations");
        Scope demandHmNominationDecide = upsertScope("DEMAND_HM_NOMINATION_DECIDE", "HM accept/reject internal nomination");

        Role adminRole = upsertRole("ADMIN");
        addScopes(adminRole, userCreate, userDelete, userView, demandCreate, demandUpdate, demandDelete,
                demandView, demandStatusTransition, demandPipelineView, demandPmApprove,
                demandSubmit, demandNominate, demandHmNominationDecide);

        Role projectManagerRole = upsertRole("PROJECT_MANAGER");
        addScopes(projectManagerRole, demandView, demandPipelineView, demandPmApprove,
                demandStatusTransition, demandSubmit);

        Role hiringManagerRole = upsertRole("HIRING_MANAGER");
        addScopes(hiringManagerRole, demandView, demandCreate, demandUpdate, demandSubmit,
                demandStatusTransition, demandPipelineView, demandHmNominationDecide);

        Role resourceManagerRole = upsertRole("RESOURCE_MANAGER");
        addScopes(resourceManagerRole, demandView, demandStatusTransition, demandNominate, demandPipelineView);

        Role recruiterRole = upsertRole("RECRUITER");
        addScopes(recruiterRole, demandView, demandStatusTransition, demandPipelineView);

        Role taManagerRole = upsertRole("TA_MANAGER");
        addScopes(taManagerRole, demandView, demandStatusTransition, demandPipelineView);

        Role rmgRole = upsertRole("RMG");
        addScopes(rmgRole, demandView, demandStatusTransition, demandPipelineView,
                demandNominate, demandCreate, demandUpdate);

        if (!userRepository.existsByEmail("username@griddynamics.com")) {
            userRepository.save(User.builder()
                    .username("User-Name")
                    .email("username@griddynamics.com")
                    .password(passwordEncoder.encode("Password@123"))
                    .enabled(true)
                    .roles(Set.of(adminRole))
                    .build());
            log.info("Default admin user created.");
        }

        if (!userRepository.existsByEmail("projectmanager@griddynamics.com")) {
            userRepository.save(User.builder()
                    .username("PM-User")
                    .email("projectmanager@griddynamics.com")
                    .password(passwordEncoder.encode("Password@123"))
                    .enabled(true)
                    .roles(Set.of(projectManagerRole))
                    .build());
            log.info("Default project manager user created.");
        }

        if (!userRepository.existsByEmail("hm@griddynamics.com")) {
            userRepository.save(User.builder()
                    .username("HM-User")
                    .email("hm@griddynamics.com")
                    .password(passwordEncoder.encode("Password@123"))
                    .enabled(true)
                    .roles(Set.of(hiringManagerRole))
                    .build());
            log.info("Default hiring manager user created.");
        }

        if (!userRepository.existsByEmail("rm@griddynamics.com")) {
            userRepository.save(User.builder()
                    .username("RM-User")
                    .email("rm@griddynamics.com")
                    .password(passwordEncoder.encode("Password@123"))
                    .enabled(true)
                    .roles(Set.of(resourceManagerRole))
                    .build());
            log.info("Default resource manager user created.");
        }
    }

    private Scope upsertScope(String name, String description) {
        return scopeRepository.findByName(name).orElseGet(() ->
                scopeRepository.save(Scope.builder().name(name).description(description).build()));
    }

    private Role upsertRole(String name) {
        return roleRepository.findByName(name).orElseGet(() ->
                roleRepository.save(Role.builder().name(name).scopes(new HashSet<>()).build()));
    }

    private void addScopes(Role role, Scope... scopes) {
        boolean changed = false;
        for (Scope s : scopes) {
            if (!role.getScopes().contains(s)) {
                role.getScopes().add(s);
                changed = true;
            }
        }
        if (changed) {
            roleRepository.save(role);
        }
    }
}
