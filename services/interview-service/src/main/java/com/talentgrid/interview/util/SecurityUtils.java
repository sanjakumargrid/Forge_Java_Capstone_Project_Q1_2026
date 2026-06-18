

package com.talentgrid.interview.util;

import com.talentgrid.shared.auth.security.JwtPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {

    private SecurityUtils() {}

    public static String getCurrentUserEmail() {
        JwtPrincipal p = getPrincipal();
        return p != null ? p.getEmail() : "system@griddynamics.com";
    }

    public static Long getCurrentUserId() {
        JwtPrincipal p = getPrincipal();
        return p != null ? p.getUserId() : 1L;
    }

    private static JwtPrincipal getPrincipal() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof JwtPrincipal jwtPrincipal) {
            return jwtPrincipal;
        }
        return null;
    }
}