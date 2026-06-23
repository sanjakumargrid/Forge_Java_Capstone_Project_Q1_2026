package com.talentgrid.auth.controller;

import com.talentgrid.auth.dto.request.CreateProjectRequest;
import com.talentgrid.auth.dto.request.UpdateProjectRequest;
import com.talentgrid.auth.dto.response.ProjectResponse;
import com.talentgrid.auth.entity.User;
import com.talentgrid.auth.repository.UserRepository;
import com.talentgrid.auth.service.interfaces.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for project CRUD and internal lookup operations.
 *
 * <p>Authorization rules:
 * <ul>
 *   <li>Create: administrator or account manager of the owning account</li>
 *   <li>Read list / update / delete: administrator or account manager of the related account</li>
 *   <li>Read by id / mine-as-pm: any authenticated caller (supports inter-service lookups)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final UserRepository userRepository;

    /**
     * Creates a new project under an account.
     *
     * POST /api/v1/projects
     */
    @PostMapping
    @PreAuthorize("@resourceAuthorizationService.canManageAccount(#request.accountId)")
    public ResponseEntity<ProjectResponse> createProject(
            @Valid @RequestBody CreateProjectRequest request
    ) {
        return ResponseEntity.ok(projectService.createProject(request));
    }

    /**
     * Projects where the authenticated user is the PM ({@code project_manager_id}).
     *
     * <p>Declared before {@code /{id}} so the path is not captured as a numeric id.
     *
     * GET /api/v1/projects/mine-as-pm
     */
    @GetMapping("/mine-as-pm")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ProjectResponse>> myProjectsAsPm() {
        User user = resolveAuthenticatedUser();
        return ResponseEntity.ok(projectService.getProjectsByProjectManagerId(user.getId()));
    }

    /**
     * Returns project details including the project manager ID.
     *
     * <p>Available to any authenticated principal so downstream services can resolve project metadata.
     *
     * GET /api/v1/projects/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProjectResponse> getProjectById(@PathVariable Long id) {
        return ResponseEntity.ok(projectService.getProjectByIdForLookup(id));
    }

    /**
     * Lists projects visible to the current user.
     *
     * GET /api/v1/projects
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ProjectResponse>> getAllProjects() {
        return ResponseEntity.ok(projectService.getAllProjects());
    }

    /**
     * Updates a project.
     *
     * PUT /api/v1/projects/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("@resourceAuthorizationService.canManageProject(#id)")
    public ResponseEntity<ProjectResponse> updateProject(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProjectRequest request
    ) {
        return ResponseEntity.ok(projectService.updateProject(id, request));
    }

    /**
     * Deletes a project.
     *
     * DELETE /api/v1/projects/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("@resourceAuthorizationService.canManageProject(#id)")
    public ResponseEntity<Void> deleteProject(@PathVariable Long id) {
        projectService.deleteProject(id);
        return ResponseEntity.noContent().build();
    }

    private User resolveAuthenticatedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + email));
    }
}
