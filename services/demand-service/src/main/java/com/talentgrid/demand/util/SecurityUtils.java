package com.talentgrid.demand.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Base64;
import java.util.Map;

/**
 * Utility class to extract user details from the Spring Security Context
 * or the HTTP Request's Authorization Header (JWT).
 * 
 * <p>
 * This class currently implements a lightweight, manual Base64 JWT decoder 
 * to act as a bridge until a centralized service-level authentication layer
 * is implemented. It falls back to stubbed values if no token is present.
 */
public class SecurityUtils {

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

    /**
     * Gets the authenticated user's employee ID from the JWT token.
     * 
     * @return the employee ID, or a default/stubbed value if not authenticated
     */
    public static Long getCurrentUserId() {
        Map<String, Object> payload = getJwtPayload();
        if (payload != null) {
            if (payload.containsKey("id")) {
                return Long.valueOf(payload.get("id").toString());
            } else if (payload.containsKey("sub")) {
                return Long.valueOf(payload.get("sub").toString());
            }
        }
        // Stubbed response for now to allow local development to proceed without JWT
        return 1L;
    }

    /**
     * Gets the authenticated user's email from the JWT token.
     * 
     * @return the email, or a default/stubbed value if not authenticated
     */
    public static String getCurrentUserEmail() {
        Map<String, Object> payload = getJwtPayload();
        if (payload != null && payload.containsKey("email")) {
            return payload.get("email").toString();
        }
        return "creator@griddynamics.com";
    }

    /**
     * Gets the authenticated user's full name from the JWT token.
     *
     * @return the name, or a default/stubbed value if not authenticated
     */
    public static String getCurrentUserName() {
        Map<String, Object> payload = getJwtPayload();
        if (payload != null && payload.containsKey("name")) {
            return payload.get("name").toString();
        }
        return "Admin User";
    }
}
