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

        Scope userCreate = createScopeIfNotFound("USER_CREATE", "Create users");
        Scope userDelete = createScopeIfNotFound("USER_DELETE", "Delete users");
        Scope userView = createScopeIfNotFound("USER_VIEW", "View users");
        
        Scope demandView = createScopeIfNotFound("DEMAND_VIEW", "View demands");
        Scope demandCreate = createScopeIfNotFound("DEMAND_CREATE", "Create demands");
        Scope demandUpdate = createScopeIfNotFound("DEMAND_UPDATE", "Update demands");
        Scope demandDelete = createScopeIfNotFound("DEMAND_DELETE", "Delete demands");
        Scope demandApprove = createScopeIfNotFound("DEMAND_APPROVE", "Approve demands");
        Scope demandStatusTransition = createScopeIfNotFound("DEMAND_STATUS_TRANSITION", "Demand Status Transitions");
        Scope demandPipelineView = createScopeIfNotFound("DEMAND_PIPELINE_VIEW", "View demands pipeline");

        // Application Scopes
        Scope applicationCreate = createScopeIfNotFound("APPLICATION_CREATE", "Create applications");
        Scope applicationView = createScopeIfNotFound("APPLICATION_VIEW", "View applications");
        Scope applicationUpdate = createScopeIfNotFound("APPLICATION_UPDATE", "Update applications");
        Scope applicationDelete = createScopeIfNotFound("APPLICATION_DELETE", "Delete applications");

        // Candidate Scopes
        Scope candidateCreate = createScopeIfNotFound("CANDIDATE_CREATE", "Create candidates");
        Scope candidateView = createScopeIfNotFound("CANDIDATE_VIEW", "View candidates");
        Scope candidateUpdate = createScopeIfNotFound("CANDIDATE_UPDATE", "Update candidates");
        Scope candidateDelete = createScopeIfNotFound("CANDIDATE_DELETE", "Delete candidates");

        // Interview Scopes
        Scope interviewCreate = createScopeIfNotFound("INTERVIEW_CREATE", "Create interviews");
        Scope interviewView = createScopeIfNotFound("INTERVIEW_VIEW", "View interviews");
        Scope interviewUpdate = createScopeIfNotFound("INTERVIEW_UPDATE", "Update interviews");
        Scope interviewDelete = createScopeIfNotFound("INTERVIEW_DELETE", "Delete interviews");

        // Scorecard Scopes
        Scope scorecardCreate = createScopeIfNotFound("SCORECARD_CREATE", "Create scorecards");
        Scope scorecardView = createScopeIfNotFound("SCORECARD_VIEW", "View scorecards");
        Scope scorecardUpdate = createScopeIfNotFound("SCORECARD_UPDATE", "Update scorecards");
        Scope scorecardDelete = createScopeIfNotFound("SCORECARD_DELETE", "Delete scorecards");

        // Offer Scopes
        Scope offerCreate = createScopeIfNotFound("OFFER_CREATE", "Create offers");
        Scope offerView = createScopeIfNotFound("OFFER_VIEW", "View offers");
        Scope offerUpdate = createScopeIfNotFound("OFFER_UPDATE", "Update offers");
        Scope offerDelete = createScopeIfNotFound("OFFER_DELETE", "Delete offers");

        // =========================================
        // CREATE OR UPDATE ADMIN ROLE
        // =========================================

        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseGet(() -> Role.builder()
                        .name("ADMIN")
                        .scopes(new HashSet<>())
                        .build());

        if (adminRole.getScopes() == null) {
            adminRole.setScopes(new HashSet<>());
        }

        adminRole.getScopes().addAll(Set.of(
                userCreate, userDelete, userView,
                demandCreate, demandUpdate, demandDelete, demandView, demandApprove, demandStatusTransition, demandPipelineView,
                applicationCreate, applicationView, applicationUpdate, applicationDelete,
                candidateCreate, candidateView, candidateUpdate, candidateDelete,
                interviewCreate, interviewView, interviewUpdate, interviewDelete,
                scorecardCreate, scorecardView, scorecardUpdate, scorecardDelete,
                offerCreate, offerView, offerUpdate, offerDelete
        ));

        Role savedAdminRole = roleRepository.save(adminRole);

        // =========================================
        // CREATE ADMIN USER
        // =========================================

        if (!userRepository.existsByEmail("admin@griddynamics.com")) {
            User admin = User.builder()
                    .username("admin")
                    .email("admin@griddynamics.com")
                    .password(passwordEncoder.encode("password"))
                    // ...

                    .enabled(true)
                    .roles(Set.of(adminRole))
                    .build();

            userRepository.save(admin);

            log.info("Default admin user created.");
        }
    }

    private Scope createScopeIfNotFound(String name, String description) {
        return scopeRepository.findByName(name)
                .orElseGet(() -> scopeRepository.save(
                        Scope.builder()
                                .name(name)
                                .description(description)
                                .build()
                ));
    }
}