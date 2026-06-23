package com.talentgrid.auth.service.interfaces;

import com.talentgrid.auth.dto.request.CreateUserAccountAssignmentRequest;
import com.talentgrid.auth.dto.request.UpdateUserAccountAssignmentRequest;
import com.talentgrid.auth.dto.response.UserAccountAssignmentResponse;

import java.util.List;

/**
 * Service contract for user-account assignment CRUD operations.
 */
public interface UserAccountAssignmentService {

    /**
     * Creates a new assignment. Caller must be an administrator.
     *
     * @param request validated create payload
     * @return created assignment
     */
    UserAccountAssignmentResponse createAssignment(CreateUserAccountAssignmentRequest request);

    /**
     * Returns an assignment by id. Caller must be an administrator.
     *
     * @param id assignment id
     * @return assignment response
     */
    UserAccountAssignmentResponse getAssignmentById(Long id);

    /**
     * Lists all assignments. Caller must be an administrator.
     *
     * @return all assignments
     */
    List<UserAccountAssignmentResponse> getAllAssignments();

    /**
     * Updates an assignment. Caller must be an administrator.
     *
     * @param id      assignment id
     * @param request validated update payload
     * @return updated assignment
     */
    UserAccountAssignmentResponse updateAssignment(Long id, UpdateUserAccountAssignmentRequest request);

    /**
     * Deletes an assignment. Caller must be an administrator.
     *
     * @param id assignment id
     */
    void deleteAssignment(Long id);
}
