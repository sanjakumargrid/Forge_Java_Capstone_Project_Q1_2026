package com.talentgrid.demand.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.talentgrid.shared.auth.security.JwtPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Utility class to extract user details from the Spring Security Context
 * or the HTTP Request's Authorization Header (JWT).
 *
 * <p>Role checks should use the same role names as {@code user-auth-service} puts on the JWT
 * (see {@code JwtService}: {@code role.getName()}), e.g. {@code HIRING_MANAGER}. The alias
 * {@code HM} is still accepted for backward compatibility with older tokens or data.
 * Portfolio manager role is {@code PORTFOLIO_MANAGER} ({@code PROJECT_MANAGER} legacy alias).
 */
public class SecurityUtils {

    /** Canonical JWT role from user-auth; {@code HM} kept as legacy alias. */
    public static final String ROLE_HIRING_MANAGER = "HIRING_MANAGER";
    public static final String ROLE_HIRING_MANAGER_ALIAS = "HM";

    /** Canonical portfolio manager role; {@code PROJECT_MANAGER} kept as legacy alias. */
    public static final String ROLE_PORTFOLIO_MANAGER = "PORTFOLIO_MANAGER";
    public static final String ROLE_PORTFOLIO_MANAGER_ALIAS = "PROJECT_MANAGER";

    public static final String ROLE_RESOURCE_MANAGER = "RESOURCE_MANAGER";
    public static final String ROLE_RESOURCE_MANAGER_ALIAS = "RM";

    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_RECRUITER = "RECRUITER";
    public static final String ROLE_TA_MANAGER = "TA_MANAGER";
    public static final String ROLE_EMPLOYEE = "EMPLOYEE";
    /** Org-wide workforce operator; treated like RM for demand lifecycle. */
    public static final String ROLE_RMG = "RMG";

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private SecurityUtils() {
        // Utility class
    }

    /**
     * Extracts and decodes the JWT payload from the current HTTP Request.
     */
    private static Map<String, Object> getJwtPayload() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String authHeader = request.getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    String token = authHeader.substring(7);
                    String[] parts = token.split("\\.");
                    if (parts.length >= 2) {
                        String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
                        return objectMapper.readValue(payload, new TypeReference<Map<String, Object>>() {});
                    }
                }
            }
        } catch (Exception e) {
            // If decoding fails, silently fallback to stubbed values
        }
        return null;
    }

    private static Map<String, Object> getUserObject() {
        Map<String, Object> payload = getJwtPayload();
        if (payload != null && payload.containsKey("user")) {
            Object userObj = payload.get("user");
            if (userObj instanceof Map) {
                return (Map<String, Object>) userObj;
            }
        }
        return payload;
    }

    /**
     * Gets the authenticated user's ID.
     */
    public static Long getCurrentUserId() {
        JwtPrincipal p = getPrincipal();
        return p != null ? p.getUserId() : 10L;
    }

    public static String getCurrentUserEmail() {
        JwtPrincipal p = getPrincipal();
        return p != null ? p.getEmail() : "testuser@griddynamics.com";
    }

    public static String getCurrentUserName() {
        JwtPrincipal p = getPrincipal();
        return p != null ? p.getEmail() : "Admin User"; // using email as name fallback
    }

    private static JwtPrincipal getPrincipal() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof JwtPrincipal jwtPrincipal) {
            return jwtPrincipal;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static List<String> getCurrentUserRoles() {
        JwtPrincipal principal = getPrincipal();
        if (principal != null && principal.getRoles() != null && !principal.getRoles().isEmpty()) {
            return principal.getRoles();
        }
        Map<String, Object> user = getUserObject();
        if (user != null && user.containsKey("roles")) {
            Object rolesObj = user.get("roles");
            if (rolesObj instanceof List) {
                return (List<String>) rolesObj;
            }
        }
        // Default fallback for local testing without valid token
        return Collections.singletonList("ADMIN");
    }

    public static boolean hasAnyRole(String... roles) {
        List<String> userRoles = getCurrentUserRoles();
        if (userRoles == null) {
            return false;
        }

        for (String role : roles) {
            if (userRoles.contains(role)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Hiring Manager per workflow: matches JWT role {@link #ROLE_HIRING_MANAGER} (issued by user-auth)
     * or legacy alias {@link #ROLE_HIRING_MANAGER_ALIAS}.
     */
    public static boolean isHiringManager() {
        return hasAnyRole(ROLE_HIRING_MANAGER, ROLE_HIRING_MANAGER_ALIAS);
    }

    public static boolean isPortfolioManager() {
        return hasAnyRole(ROLE_PORTFOLIO_MANAGER, ROLE_PORTFOLIO_MANAGER_ALIAS);
    }

    public static boolean isResourceManager() {
        return hasAnyRole(ROLE_RESOURCE_MANAGER, ROLE_RESOURCE_MANAGER_ALIAS, ROLE_RMG);
    }

    public static boolean isRecruiter() {
        return hasAnyRole(ROLE_RECRUITER);
    }

    public static boolean isTaManager() {
        return hasAnyRole(ROLE_TA_MANAGER);
    }

    public static boolean isEmployee() {
        return hasAnyRole(ROLE_EMPLOYEE);
    }

    /** Platform admin — may perform any state-machine-legal transition. */
    public static boolean isPlatformAdmin() {
        return hasAnyRole(ROLE_ADMIN);
    }

    public static Long getCurrentUserAccountId() {
        Map<String, Object> user = getUserObject();
        if (user != null && user.containsKey("AccountId")) {
            try {
                return Long.valueOf(user.get("AccountId").toString());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
