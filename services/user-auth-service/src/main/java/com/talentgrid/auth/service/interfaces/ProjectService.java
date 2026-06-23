package com.talentgrid.auth.service.interfaces;

import com.talentgrid.auth.dto.request.CreateProjectRequest;
import com.talentgrid.auth.dto.request.UpdateProjectRequest;
import com.talentgrid.auth.dto.response.ProjectResponse;

import java.util.List;

/**
 * Service contract for project CRUD operations.
 */
public interface ProjectService {

    /**
     * Creates a project under an account when the caller can manage the account.
     *
     * @param request validated create payload
     * @return created project
     */
    ProjectResponse createProject(CreateProjectRequest request);

    /**
     * Returns a project by id after authorization checks.
     *
     * @param id project id
     * @return project response
     */
    ProjectResponse getProjectById(Long id);

    /**
     * Returns a project by id for authenticated internal lookups (no ownership check).
     *
     * @param id project id
     * @return project response
     */
    ProjectResponse getProjectByIdForLookup(Long id);

    /**
     * Lists projects visible to the current user.
     *
     * @return authorized project list
     */
    List<ProjectResponse> getAllProjects();

    /**
     * Updates a project when the caller is authorized.
     *
     * @param id      project id
     * @param request validated update payload
     * @return updated project
     */
    ProjectResponse updateProject(Long id, UpdateProjectRequest request);

    /**
     * Deletes a project when the caller is authorized.
     *
     * @param id project id
     */
    void deleteProject(Long id);

    /**
     * Returns projects where the given user is the project manager.
     *
     * @param projectManagerId user id
     * @return PM-scoped project summaries
     */
    List<ProjectResponse> getProjectsByProjectManagerId(Long projectManagerId);
}
