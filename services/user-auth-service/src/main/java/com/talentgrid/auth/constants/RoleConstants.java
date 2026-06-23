package com.talentgrid.auth.constants;

/**
 * Well-known role name used in authorization checks.
 *
 * <p>Roles are defined in the database and embedded in JWTs at login time
 * ({@code roles} claim → {@code ROLE_*} Spring authorities). This constant is
 * <strong>not</strong> a second source of role definitions — it only avoids
 * scattering the {@code ADMIN} string in {@code @PreAuthorize} and service code.
 *
 * <p>Account-manager access is <em>not</em> a role; it is resolved from
 * {@code accounts.account_manager_id} via {@link com.talentgrid.auth.service.interfaces.ResourceAuthorizationService}.
 */
public final class RoleConstants {

    /** System administrator role name (must match the {@code roles} table). */
    public static final String ADMIN = "ADMIN";

    private RoleConstants() {
    }

    /**
     * Converts a database role name to the Spring Security authority format.
     *
     * @param roleName role name from JWT / database (e.g. {@link #ADMIN})
     * @return authority string (e.g. {@code ROLE_ADMIN})
     */
    public static String toAuthority(String roleName) {
        return "ROLE_" + roleName;
    }
}
