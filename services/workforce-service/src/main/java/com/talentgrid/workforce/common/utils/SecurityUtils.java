package com.talentgrid.workforce.common.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.talentgrid.shared.auth.security.JwtPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Base64;
import java.util.Map;

public class SecurityUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private SecurityUtils() {}

    public static String getCurrentUserEmail() {
        JwtPrincipal p = getPrincipal();
        if (p != null) {
            return p.getEmail();
        }
        Map<String, Object> payload = getJwtPayload();
        if (payload != null && payload.containsKey("email")) {
            return payload.get("email").toString();
        }
        return "system@griddynamics.com";
    }

    public static Long getCurrentUserId() {
        JwtPrincipal p = getPrincipal();
        if (p != null) {
            return p.getUserId();
        }
        Map<String, Object> payload = getJwtPayload();
        if (payload != null && payload.containsKey("sub")) {
            try {
                return Long.parseLong(payload.get("sub").toString());
            } catch (NumberFormatException e) {
                // Ignore
            }
        }
        return 1L;
    }

    private static JwtPrincipal getPrincipal() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof JwtPrincipal jwtPrincipal) {
            return jwtPrincipal;
        }
        return null;
    }

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
                        String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), java.nio.charset.StandardCharsets.UTF_8);
                        return objectMapper.readValue(payloadJson, new TypeReference<Map<String, Object>>() {});
                    }
                }
            }
        } catch (Exception e) {
            // Silently fallback
        }
        return null;
    }
}
