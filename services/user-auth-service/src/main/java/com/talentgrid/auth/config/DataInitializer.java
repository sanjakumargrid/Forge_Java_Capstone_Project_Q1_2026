package com.talentgrid.auth.config;

import com.talentgrid.auth.entity.Role;
import com.talentgrid.auth.entity.Scope;
import com.talentgrid.auth.entity.User;
import com.talentgrid.auth.entity.Account;
import com.talentgrid.auth.entity.Project;
import com.talentgrid.auth.entity.BusinessUnit;
import com.talentgrid.auth.entity.AccountBusinessUnitMapping;
import com.talentgrid.auth.entity.Location;
import com.talentgrid.auth.entity.Department;
import com.talentgrid.auth.repository.AccountRepository;
import com.talentgrid.auth.repository.ProjectRepository;
import com.talentgrid.auth.repository.RoleRepository;
import com.talentgrid.auth.repository.ScopeRepository;
import com.talentgrid.auth.repository.UserRepository;
import com.talentgrid.auth.repository.BusinessUnitRepository;
import com.talentgrid.auth.repository.AccountBusinessUnitMappingRepository;
import com.talentgrid.auth.repository.LocationRepository;
import com.talentgrid.auth.repository.DepartmentRepository;
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
    private final AccountRepository accountRepository;
    private final ProjectRepository projectRepository;
    private final BusinessUnitRepository businessUnitRepository;
    private final AccountBusinessUnitMappingRepository accountBusinessUnitMappingRepository;
    private final LocationRepository locationRepository;
    private final DepartmentRepository departmentRepository;

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

        Scope candidateView = upsertScope("CANDIDATE_VIEW", "Candidate View");
        Scope candidateDelete = upsertScope("CANDIDATE_DELETE", "Candidate delete");

        Scope applicationView = upsertScope("APPLICATION_VIEW", "Application view");
        Scope applicationUpdate = upsertScope("APPLICATION_UPDATE", "Application update");

        Scope interviewCreate = upsertScope("INTERVIEW_CREATE", "Interview Create");
        Scope interviewView = upsertScope("INTERVIEW_VIEW", "Interview view");
        Scope interviewUpdate = upsertScope("INTERVIEW_UPDATE", "Interview update");
        Scope interviewDelete = upsertScope("INTERVIEW_DELETE", "Interview Delete");

        Scope offerCreate = upsertScope("OFFER_CREATE", "Offer create");
        Scope offerView = upsertScope("OFFER_VIEW", "Offer view");
        Scope offerUpdate = upsertScope("OFFER_UPDATE", "Offer update");
        Scope offerDelete = upsertScope("OFFER_DELETE", "Offer delete");

        Role adminRole = upsertRole("ADMIN");
        addScopes(adminRole,
                userCreate, userDelete, userView,

                demandCreate, demandUpdate, demandDelete,
                demandView, demandStatusTransition, demandPipelineView,
                demandPmApprove, demandSubmit, demandNominate, demandHmNominationDecide,

                candidateView, candidateDelete,

                applicationView, applicationUpdate,

                interviewCreate, interviewView, interviewUpdate, interviewDelete,

                offerCreate, offerView, offerUpdate, offerDelete
        );
        Role portfolioManagerRole = upsertRole("PORTFOLIO_MANAGER");
        addScopes(portfolioManagerRole, demandView, demandPipelineView, demandPmApprove,
                demandStatusTransition, demandSubmit);

        Role hiringManagerRole = upsertRole("HIRING_MANAGER");
        addScopes(hiringManagerRole, demandView, demandCreate, demandUpdate, demandSubmit,
                demandStatusTransition, demandPipelineView, demandHmNominationDecide);

        Role resourceManagerRole = upsertRole("RESOURCE_MANAGER");
        addScopes(resourceManagerRole, demandView, demandStatusTransition, demandNominate, demandPipelineView,
                demandCreate, demandUpdate);

        Role recruiterRole = upsertRole("RECRUITER");
        addScopes(recruiterRole,
                demandView, demandStatusTransition, demandPipelineView,
                candidateView, candidateDelete,

                applicationView, applicationUpdate,

                interviewCreate, interviewView, interviewUpdate, interviewDelete,

                offerCreate, offerView, offerUpdate, offerDelete
        );

        Role taManagerRole = upsertRole("TA_MANAGER");
        addScopes(taManagerRole, demandView, demandStatusTransition, demandPipelineView);

        Role employeeRole = upsertRole("EMPLOYEE");
        addScopes(employeeRole, demandView);

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
                    .roles(Set.of(portfolioManagerRole))
                    .build());
            log.info("Default portfolio manager user created.");
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
                    .location("Chennai")
                    .password(passwordEncoder.encode("Password@123"))
                    .enabled(true)
                    .roles(Set.of(resourceManagerRole))
                    .build());
            log.info("Default resource manager user created.");
        }

        // Create Default Account
        Account account = null;
        if (accountRepository.count() == 0) {
            account = Account.builder()
                    .name("Mock Account")
                    .accountManagerId(1L) // Admin user
                    .build();
            account = accountRepository.save(account);
            log.info("Default account created.");
        } else {
            account = accountRepository.findAll().get(0);
        }

        // Create Default Project
        if (projectRepository.count() == 0) {
            User pmUser = userRepository.findByEmail("projectmanager@griddynamics.com").orElse(null);
            if (pmUser != null) {
                Project project = Project.builder()
                        .name("Unknown Project")
                        .account(account)
                        .projectManagerId(pmUser.getId())
                        .build();
                projectRepository.save(project);
                log.info("Default project created.");
            }
        }

        // ── Seed Business Units ───────────────────────────────────────────────────
        if (businessUnitRepository.count() == 0) {
            BusinessUnit engBu = businessUnitRepository.save(
                    BusinessUnit.builder().businessUnitName("Engineering").build());
            BusinessUnit hrBu  = businessUnitRepository.save(
                    BusinessUnit.builder().businessUnitName("Human Resources").build());
            BusinessUnit finBu = businessUnitRepository.save(
                    BusinessUnit.builder().businessUnitName("Finance").build());
            BusinessUnit opsBu = businessUnitRepository.save(
                    BusinessUnit.builder().businessUnitName("Operations").build());

            // Map BUs to the default account (account variable is already declared above)
            accountBusinessUnitMappingRepository.save(
                    AccountBusinessUnitMapping.builder().account(account).businessUnit(engBu).build());
            accountBusinessUnitMappingRepository.save(
                    AccountBusinessUnitMapping.builder().account(account).businessUnit(hrBu).build());
            accountBusinessUnitMappingRepository.save(
                    AccountBusinessUnitMapping.builder().account(account).businessUnit(finBu).build());
            accountBusinessUnitMappingRepository.save(
                    AccountBusinessUnitMapping.builder().account(account).businessUnit(opsBu).build());
            log.info("Default business units seeded.");
        }

        // ── Seed Locations ────────────────────────────────────────────────────────
        if (locationRepository.count() == 0) {
            locationRepository.save(Location.builder().country("India").locationName("Chennai").build());
            locationRepository.save(Location.builder().country("India").locationName("Bengaluru").build());
            locationRepository.save(Location.builder().country("India").locationName("Hyderabad").build());
            locationRepository.save(Location.builder().country("USA").locationName("Atlanta").build());
            locationRepository.save(Location.builder().country("USA").locationName("New York").build());
            locationRepository.save(Location.builder().country("UK").locationName("London").build());
            log.info("Default locations seeded.");
        }

        // ── Seed Departments ──────────────────────────────────────────────────────
        if (departmentRepository.count() == 0) {
            departmentRepository.save(Department.builder().departmentName("Software Engineering").build());
            departmentRepository.save(Department.builder().departmentName("Quality Assurance").build());
            departmentRepository.save(Department.builder().departmentName("DevOps").build());
            departmentRepository.save(Department.builder().departmentName("Product Management").build());
            departmentRepository.save(Department.builder().departmentName("Data Science").build());
            departmentRepository.save(Department.builder().departmentName("Human Resources").build());
            departmentRepository.save(Department.builder().departmentName("Finance").build());
            log.info("Default departments seeded.");
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
