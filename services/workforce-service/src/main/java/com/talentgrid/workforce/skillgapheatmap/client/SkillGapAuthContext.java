package com.talentgrid.workforce.skillgapheatmap.client;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Resolves the caller's JWT from the current HTTP request.
 * Used to propagate user authentication to demand-service on on-demand refresh.
 */
public final class SkillGapAuthContext {

    private SkillGapAuthContext() {
    }

    public static boolean hasAuthorization() {
        return resolveAuthorizationHeader() != null;
    }

    public static String resolveAuthorizationHeader() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return null;
        }
        return authorizationHeader;
    }
}
