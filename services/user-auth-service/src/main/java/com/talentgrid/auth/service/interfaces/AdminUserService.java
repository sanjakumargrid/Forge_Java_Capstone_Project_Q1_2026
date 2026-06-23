package com.talentgrid.auth.service.interfaces;

import com.talentgrid.auth.dto.request.AdminCreateUserRequest;
import com.talentgrid.auth.dto.request.AdminUpdateUserRequest;
import com.talentgrid.auth.dto.request.ChangeRoleRequest;
import com.talentgrid.auth.dto.response.AdminUserResponse;

import java.util.List;

/**
 * Service interface for Admin User Management.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Provide CRUD operations for users via admin endpoints</li>
 *   <li>Manage role assignments and account status (enable/disable)</li>
 *   <li>Ensure session invalidation upon status or role changes</li>
 * </ul>
 */
public interface AdminUserService {

    /**
     * Creates a new user with a specified role.
     *
     * @param request the user creation details
     * @return the created user details
     */
    AdminUserResponse createUser(AdminCreateUserRequest request);

    /**
     * Updates an existing user's details.
     *
     * @param id      the user ID
     * @param request the updated details
     * @return the updated user
     */
    AdminUserResponse updateUser(Long id, AdminUpdateUserRequest request);

    /**
     * Soft-deletes a user by disabling their account and revoking tokens.
     *
     * @param id the user ID
     */
    void deleteUser(Long id);

    /**
     * Retrieves a user by their ID.
     *
     * @param id the user ID
     * @return the user details
     */
    AdminUserResponse getUserById(Long id);

    /**
     * Retrieves all users in the system.
     *
     * @return a list of all users
     */
    List<AdminUserResponse> getAllUsers();

    /**
     * Changes the role of an existing user and invalidates their current session.
     *
     * @param id      the user ID
     * @param request the new role details
     * @return the updated user
     */
    AdminUserResponse changeUserRole(Long id, ChangeRoleRequest request);

    /**
     * Enables or disables a user account and invalidates their current session if disabled.
     *
     * @param id      the user ID
     * @param enabled the new status
     * @return the updated user
     */
    AdminUserResponse toggleUserStatus(Long id, boolean enabled);
}
