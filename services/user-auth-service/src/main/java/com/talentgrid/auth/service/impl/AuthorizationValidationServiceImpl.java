package com.talentgrid.auth.service.impl;

import com.talentgrid.auth.dto.cache.CachedUserContext;
import com.talentgrid.auth.service.interfaces.AuthorizationValidationService;
import com.talentgrid.auth.service.interfaces.UserSecurityCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Service to validate incoming JWT authorization state against the
 * current Redis cache state.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Ensure the incoming JWT's authVersion matches the user's current version</li>
 *   <li>Ensure the user's account is still enabled</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class AuthorizationValidationServiceImpl
        implements AuthorizationValidationService {

    private final UserSecurityCacheService
            userSecurityCacheService;

    @Override
    public boolean isVersionValid(
            Long userId,
            Long jwtVersion
    ) {

        CachedUserContext cached =
                userSecurityCacheService
                        .getUser(userId);

        if (cached == null) {
            return false; // Cache miss requires re-login
        }
        
        if (!Boolean.TRUE.equals(cached.getEnabled())) {
            return false; // User is disabled
        }

        if (cached.getAuthVersion() == null) {
            return false;
        }

        return cached.getAuthVersion()
                .equals(jwtVersion);
    }
}