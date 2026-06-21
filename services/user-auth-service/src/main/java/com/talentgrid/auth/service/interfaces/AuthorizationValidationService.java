package com.talentgrid.auth.service.interfaces;

/**
 * Service interface for validating authorization state.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Ensure the incoming JWT's authVersion matches the user's current version</li>
 *   <li>Ensure the user's account is still enabled</li>
 * </ul>
 */
public interface AuthorizationValidationService {

    /**
     * Checks if the provided JWT version matches the current cached version
     * and if the user is still enabled.
     *
     * @param userId     the user's database identifier
     * @param jwtVersion the authVersion extracted from the JWT
     * @return true if the version matches and the user is enabled
     */
    boolean isVersionValid(
            Long userId,
            Long jwtVersion
    );
}