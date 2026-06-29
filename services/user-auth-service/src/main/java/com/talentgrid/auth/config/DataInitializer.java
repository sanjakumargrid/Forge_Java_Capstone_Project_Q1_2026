package com.talentgrid.auth.config;

import com.talentgrid.auth.constants.ScopeCatalog;
import com.talentgrid.auth.entity.Account;
import com.talentgrid.auth.entity.AccountBusinessUnitMapping;
import com.talentgrid.auth.entity.BusinessUnit;
import com.talentgrid.auth.entity.Department;
import com.talentgrid.auth.entity.Location;
import com.talentgrid.auth.entity.Project;
import com.talentgrid.auth.entity.Role;
import com.talentgrid.auth.entity.Scope;
import com.talentgrid.auth.entity.User;
import com.talentgrid.auth.repository.AccountBusinessUnitMappingRepository;
import com.talentgrid.auth.repository.AccountRepository;
import com.talentgrid.auth.repository.BusinessUnitRepository;
import com.talentgrid.auth.repository.DepartmentRepository;
import com.talentgrid.auth.repository.LocationRepository;
import com.talentgrid.auth.repository.ProjectRepository;
import com.talentgrid.auth.repository.RoleRepository;
import com.talentgrid.auth.repository.ScopeRepository;
import com.talentgrid.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Seeds scopes and roles for local/dev. Align scope names with
 * {@code talentgrid-api-gateway-service} {@code rbac-rules.yml} and downstream
 * {@code @PreAuthorize}.
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
        Map<String, Scope> scopesByName = seedAllScopes();

        Role adminRole = upsertRole("ADMIN");
        addScopes(adminRole,
                ScopeCatalog.allScopeNames().stream().map(scopesByName::get).toArray(Scope[]::new));

        Role portfolioManagerRole = upsertRole("PORTFOLIO_MANAGER");
        addScopes(portfolioManagerRole, scopes(scopesByName,
                "DEMAND_VIEW", "DEMAND_CREATE", "DEMAND_UPDATE", "DEMAND_PIPELINE_VIEW",
                "DEMAND_PM_APPROVE", "DEMAND_STATUS_TRANSITION", "DEMAND_SUBMIT",
                "ANALYTICS_DEMAND_VIEW", "USER_VIEW"));

        Role hiringManagerRole = upsertRole("HIRING_MANAGER");
        addScopes(hiringManagerRole, scopes(scopesByName,
                "DEMAND_VIEW", "DEMAND_CREATE", "DEMAND_UPDATE", "DEMAND_SUBMIT",
                "DEMAND_STATUS_TRANSITION", "DEMAND_PIPELINE_VIEW", "DEMAND_HM_NOMINATION_DECIDE",
                "HM_NOMINATION_VIEW", "HM_NOMINATION_REVIEW"));

        Role resourceManagerRole = upsertRole("RESOURCE_MANAGER");
        addScopes(resourceManagerRole, scopes(scopesByName,
                "DEMAND_VIEW", "DEMAND_CREATE", "DEMAND_UPDATE", "DEMAND_STATUS_TRANSITION",
                "DEMAND_NOMINATE", "DEMAND_PIPELINE_VIEW",
                "WORKFORCE_BENCH_SEARCH", "WORKFORCE_NOMINATION_VIEW", "WORKFORCE_NOMINATION_CREATE",
                "WORKFORCE_NOMINATION_DELETE", "WORKFORCE_SKILLGAP_VIEW", "WORKFORCE_SKILLGAP_REFRESH",
                "WORKFORCE_ANALYTICS_VIEW", "WORKFORCE_REPORT_EXPORT", "WORKFORCE_AI_UPSKILL_VIEW"));

        Role recruiterRole = upsertRole("RECRUITER");
        addScopes(recruiterRole, scopes(scopesByName,
                "DEMAND_VIEW", "DEMAND_STATUS_TRANSITION", "DEMAND_PIPELINE_VIEW",
                "CANDIDATE_VIEW", "CANDIDATE_CREATE", "CANDIDATE_UPDATE", "CANDIDATE_DELETE",
                "CANDIDATE_NOTE_CREATE", "RESUME_UPLOAD",
                "APPLICATION_VIEW", "APPLICATION_CREATE", "APPLICATION_UPDATE",
                "APPLICATION_BULK_ACTION", "APPLICATION_STAGE_MOVE",
                "INTERVIEW_VIEW", "INTERVIEW_CREATE", "INTERVIEW_UPDATE", "INTERVIEW_DELETE",
                "INTERVIEW_CALENDAR_VIEW",
                "SCORECARD_CREATE", "SCORECARD_SUBMIT", "SCORECARD_VIEW", "SCORECARD_DELETE",
                "OFFER_CREATE", "OFFER_VIEW", "OFFER_UPDATE", "OFFER_DELETE",
                "AI_CANDIDATE_SCORE", "AI_REJECTION_EMAIL_GENERATE", "AI_REJECTION_EMAIL_SEND",
                "AI_INTERVIEW_QUESTIONS"));

        Role taManagerRole = upsertRole("TA_MANAGER");
        addScopes(taManagerRole, scopes(scopesByName,
                "DEMAND_VIEW", "DEMAND_STATUS_TRANSITION", "DEMAND_PIPELINE_VIEW",
                "CANDIDATE_VIEW", "CANDIDATE_CREATE", "CANDIDATE_UPDATE", "CANDIDATE_DELETE",
                "APPLICATION_VIEW", "APPLICATION_CREATE", "APPLICATION_UPDATE",
                "INTERVIEW_VIEW", "INTERVIEW_CREATE", "INTERVIEW_UPDATE", "INTERVIEW_DELETE",
                "SCORECARD_CREATE", "SCORECARD_SUBMIT", "SCORECARD_VIEW", "SCORECARD_DELETE",
                "OFFER_CREATE", "OFFER_VIEW", "OFFER_UPDATE", "OFFER_DELETE",
                "ANALYTICS_PIPELINE_VIEW"));

        Role employeeRole = upsertRole("EMPLOYEE");
        addScopes(employeeRole, scopes(scopesByName,
                "DEMAND_VIEW", "SCORECARD_VIEW", "SCORECARD_CREATE", "SCORECARD_DELETE",
                "APPLICATION_VIEW", "ENGINEER_SELF_UPDATE", "WORKFORCE_AI_UPSKILL_VIEW",
                "WORKFORCE_PROFILE_VIEW", "WORKFORCE_PROFILE_UPDATE"));

        seedDefaultUsers(adminRole, portfolioManagerRole, hiringManagerRole, resourceManagerRole);
        seedReferenceData();
    }

    private Map<String, Scope> seedAllScopes() {
        Map<String, Scope> scopesByName = new HashMap<>();
        ScopeCatalog.allScopes()
                .forEach((name, description) -> scopesByName.put(name, upsertScope(name, description)));
        return scopesByName;
    }

    private Scope[] scopes(Map<String, Scope> scopesByName, String... names) {
        return java.util.Arrays.stream(names).map(scopesByName::get).toArray(Scope[]::new);
    }

    private void seedDefaultUsers(Role adminRole, Role portfolioManagerRole,
                                  Role hiringManagerRole, Role resourceManagerRole) {
        if (!userRepository.existsByEmail("admin@griddynamics.com")) {
            userRepository.save(User.builder()
                    .username("Admin")
                    .email("admin@griddynamics.com")
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
    }

    private void seedReferenceData() {
        Account account;
        if (accountRepository.count() == 0) {
            account = accountRepository.save(Account.builder()
                    .name("Mock Account")
                    .accountManagerId(1L)
                    .build());
            log.info("Default account created.");
        } else {
            account = accountRepository.findAll().get(0);
        }

        if (projectRepository.count() == 0) {
            userRepository.findByEmail("projectmanager@griddynamics.com").ifPresent(pmUser -> {
                projectRepository.save(Project.builder()
                        .name("Unknown Project")
                        .account(account)
                        .projectManagerId(pmUser.getId())
                        .build());
                log.info("Default project created.");
            });
        }

        if (businessUnitRepository.count() == 0) {
            List<BusinessUnit> businessUnits = List.of(
                            "Engineering",
                            "Human Resources",
                            "Finance & Accounting",
                            "Operations",
                            "Sales",
                            "Marketing",
                            "Legal",
                            "IT",
                            "Talent Acquisition",
                            "Executive"
                    ).stream()
                    .map(name -> businessUnitRepository.save(
                            BusinessUnit.builder().businessUnitName(name).build()))
                    .toList();

            businessUnits.forEach(bu -> accountBusinessUnitMappingRepository.save(
                    AccountBusinessUnitMapping.builder().account(account).businessUnit(bu).build()));

            log.info("Default business units seeded ({} entries).", businessUnits.size());
        }

        if (locationRepository.count() == 0) {
            Map.of(
                    "India", List.of("Chennai", "Bengaluru", "Hyderabad"),
                    "USA", List.of("Atlanta", "New York"),
                    "UK", List.of("London")
            ).forEach((country, cities) -> cities.forEach(city ->
                    locationRepository.save(Location.builder()
                            .country(country)
                            .locationName(city)
                            .build())
            ));
            log.info("Default locations seeded ({} entries).", locationRepository.count());
        }

        if (departmentRepository.count() == 0) {
            List.of(
                    "Account Administration",
                    "Client Services",
                    "Contractors Experience and Operations",
                    "Engineering: .NET",
                    "Engineering: Analysis",
                    "Engineering: BigData",
                    "Engineering: CTO",
                    "Engineering: Custom Development",
                    "Engineering: Customer Support Analysis",
                    "Engineering: Data Analysis",
                    "Engineering: Data Science",
                    "Engineering: Design",
                    "Engineering: DevOps",
                    "Engineering: Engineering Management",
                    "Engineering: Java",
                    "Engineering: Machine Learning",
                    "Engineering: Management",
                    "Engineering: Mobile",
                    "Engineering: Other Technologies",
                    "Engineering: Physical AI",
                    "Engineering: Python",
                    "Engineering: Quality Engineering",
                    "Engineering: Search",
                    "Engineering: UI",
                    "Executive",
                    "Finance & Accounting",
                    "Global Mobility",
                    "HQ",
                    "HR Compliance and Shared Services",
                    "HR Systems and Operational Analytics",
                    "HR: Business Partnership",
                    "HR: Compensation and Benefits",
                    "HR: Culture and Engagement",
                    "HR: Training and Development",
                    "IT",
                    "Language Training",
                    "Legal Support and Contract Management",
                    "Marketing",
                    "Operations",
                    "Sales",
                    "Security",
                    "Talent Acquisition",
                    "Training"
            ).forEach(name -> departmentRepository.save(
                    Department.builder().departmentName(name).build()));
            log.info("Default departments seeded ({} entries).", departmentRepository.count());
        }
    }

    private Scope upsertScope(String name, String description) {
        return scopeRepository.findByName(name).orElseGet(() -> scopeRepository
                .save(Scope.builder().name(name).description(description).build()));
    }

    private Role upsertRole(String name) {
        return roleRepository.findByName(name).orElseGet(
                () -> roleRepository.save(Role.builder().name(name).scopes(new HashSet<>()).build()));
    }

    private void addScopes(Role role, Scope... scopes) {
        boolean changed = false;
        for (Scope scope : scopes) {
            if (scope != null && !role.getScopes().contains(scope)) {
                role.getScopes().add(scope);
                changed = true;
            }
        }
        if (changed) {
            roleRepository.save(role);
        }
    }
}
