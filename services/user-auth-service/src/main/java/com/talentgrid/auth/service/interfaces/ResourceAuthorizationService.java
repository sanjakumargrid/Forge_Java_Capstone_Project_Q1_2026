package com.talentgrid.auth.service.interfaces;

/**
 * Domain-level authorization checks for account and project management.
 *
 * <p>Used by service layers and {@code @PreAuthorize} SpEL expressions to enforce
 * role- and ownership-based access without hardcoding role strings in controllers.
 */
public interface ResourceAuthorizationService {

    /**
     * @return the authenticated user's database identifier
     */
    Long getCurrentUserId();

    /**
     * @return {@code true} when the current user has the {@code ADMIN} role
     */
    boolean isAdmin();

    /**
     * @param accountId the account identifier
     * @return {@code true} when the current user is the assigned account manager
     */
    boolean isAccountManager(Long accountId);

    /**
     * @param accountId the account identifier
     * @return {@code true} when the current user may manage the account (admin or account manager)
     */
    boolean canManageAccount(Long accountId);

    /**
     * @param projectId the project identifier
     * @return {@code true} when the current user may manage the project (admin or account manager of its account)
     */
    boolean canManageProject(Long projectId);

    /**
     * Ensures the current user may manage the given account.
     *
     * @param accountId the account identifier
     * @throws org.springframework.security.access.AccessDeniedException when access is denied
     */
    void requireCanManageAccount(Long accountId);

    /**
     * Ensures the current user may manage the given project.
     *
     * @param projectId the project identifier
     * @throws org.springframework.security.access.AccessDeniedException when access is denied
     */
    void requireCanManageProject(Long projectId);

    /**
     * Ensures the current user has administrator privileges.
     *
     * @throws org.springframework.security.access.AccessDeniedException when the user is not an admin
     */
    void requireAdmin();
}
