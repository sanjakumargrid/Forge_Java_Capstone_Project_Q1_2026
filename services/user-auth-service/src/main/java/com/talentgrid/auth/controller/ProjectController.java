package com.talentgrid.auth.controller;

import com.talentgrid.auth.entity.Project;
import com.talentgrid.auth.entity.User;
import com.talentgrid.auth.repository.ProjectRepository;
import com.talentgrid.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Internal project lookup API.
 *
 * Used by demand-service via Feign to resolve project name and
 * project manager ID during demand creation and SLA processing.
 */
@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    /**
     * Projects where the authenticated user is the PM ({@code project_manager_id}).
     * Used by demand-service to scope PM demand listings.
     *
     * <p>Declared before {@code /{id}} so the path is not captured as a numeric id.
     *
     * GET /api/v1/projects/mine-as-pm
     */
    @GetMapping("/mine-as-pm")
    public ResponseEntity<List<Map<String, Object>>> myProjectsAsPm() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + email));

        List<Map<String, Object>> body = projectRepository.findByProjectManagerId(user.getId()).stream()
                .map(this::toProjectSummary)
                .collect(Collectors.toList());

        return ResponseEntity.ok(body);
    }

    /**
     * Returns project details including the project manager ID.
     *
     * GET /api/v1/projects/{id}
     * Response: { "id": 1, "name": "Alpha", "accountId": 2, "projectManagerId": 5 }
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getProjectById(@PathVariable Long id) {
        return projectRepository.findById(id)
                .map(project -> ResponseEntity.ok(Map.of(
                        "id",               project.getId(),
                        "name",             project.getName(),
                        "accountId",        project.getAccount() != null ? project.getAccount().getId() : 0L,
                        "projectManagerId", project.getProjectManagerId() != null ? project.getProjectManagerId() : 0L
                )))
                .orElse(ResponseEntity.notFound().build());
    }

    private Map<String, Object> toProjectSummary(Project project) {
        return Map.of(
                "id", project.getId(),
                "name", project.getName(),
                "accountId", project.getAccount() != null ? project.getAccount().getId() : 0L,
                "projectManagerId", project.getProjectManagerId() != null ? project.getProjectManagerId() : 0L
        );
    }
}
