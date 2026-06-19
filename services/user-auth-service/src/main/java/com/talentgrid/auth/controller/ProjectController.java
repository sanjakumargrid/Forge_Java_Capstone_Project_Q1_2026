package com.talentgrid.auth.controller;

import com.talentgrid.auth.entity.Project;
import com.talentgrid.auth.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Internal project lookup API.
 *
 * Used by demand-service via Feign to resolve project name and
 * project manager ID during demand creation and SLA processing.
 */
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectRepository projectRepository;

    /**
     * Returns project details including the project manager ID.
     *
     * GET /api/projects/{id}
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
}