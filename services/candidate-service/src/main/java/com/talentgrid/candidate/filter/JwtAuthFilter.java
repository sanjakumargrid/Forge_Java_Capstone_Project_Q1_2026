package com.talentgrid.candidate.filter;

import com.talentgrid.shared.auth.constants.JwtConstants;
import com.talentgrid.shared.auth.security.JwtAuthenticationProvider;
import com.talentgrid.shared.auth.security.JwtPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtAuthenticationProvider jwtAuthenticationProvider;
    private final RedisTemplate<String, Object> objectRedisTemplate;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String method = request.getMethod();
        String servletPath = request.getServletPath();

        boolean isPublicCandidateCreate =
                "POST".equalsIgnoreCase(method)
                        && (
                        "/api/v1/external-candidates".equals(servletPath)
                                || "/api/v1/external-candidates/".equals(servletPath)
                );

        boolean isPublicAiEngine = servletPath.startsWith("/api/v1/aiengine");

        boolean isSwaggerOrActuator =
                servletPath.startsWith("/actuator")
                        || servletPath.startsWith("/v3/api-docs")
                        || servletPath.startsWith("/swagger-ui")
                        || "/swagger-ui.html".equals(servletPath);

        return isPublicCandidateCreate
                || isPublicAiEngine
                || isSwaggerOrActuator
                || "OPTIONS".equalsIgnoreCase(method);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            JwtPrincipal principal = jwtAuthenticationProvider.authenticate(token);

            boolean internalServiceToken = isInternalServiceToken(principal);

            if (!internalServiceToken && isRealUser(principal.getUserId())) {
                String redisKey = "auth:user:" + principal.getUserId();
                Boolean exists = objectRedisTemplate.hasKey(redisKey);

                if (!Boolean.TRUE.equals(exists)) {
                    SecurityContextHolder.clearContext();
                    sendError(response, HttpServletResponse.SC_UNAUTHORIZED,
                            "Session expired. Please login again.");
                    return;
                }
            }

            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                Set<String> authorityNames = new LinkedHashSet<>();

                if (internalServiceToken) {
                    authorityNames.add("ROLE_SYSTEM");
                    authorityNames.add("SYSTEM");
                    authorityNames.add("CANDIDATE_VIEW");
                } else {
                    addUserAuthorities(principal, authorityNames);
                }

                var authorities = authorityNames.stream()
                        .map(SimpleGrantedAuthority::new)
                        .toList();

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(principal, null, authorities);

                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.info("Authenticated candidate-service userId={}, email={}, internalServiceToken={}, authorities={}",
                        principal.getUserId(),
                        principal.getEmail(),
                        internalServiceToken,
                        authorityNames
                );
            }

        } catch (Exception e) {
            log.warn("JWT validation failed: {}", e.getMessage());

            SecurityContextHolder.clearContext();

            sendError(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Invalid or expired JWT");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void addUserAuthorities(JwtPrincipal principal, Set<String> authorityNames) {
        if (principal.getRoles() != null) {
            principal.getRoles().forEach(role -> {
                String cleanRole = normalize(role).replace("ROLE_", "");
                authorityNames.add("ROLE_" + cleanRole);

                addCandidatePermissions(cleanRole, authorityNames);
            });
        }

        if (principal.getScopes() != null) {
            principal.getScopes().forEach(scope -> addScopeAuthority(scope, authorityNames));
        }
    }

    private boolean isInternalServiceToken(JwtPrincipal principal) {
        return principal != null
                && JwtConstants.INTERNAL_SERVICE_EMAIL.equals(principal.getEmail());
    }

    private boolean isRealUser(Long userId) {
        return userId != null && userId != 0L;
    }

    private void addScopeAuthority(String scope, Set<String> authorityNames) {
        if (scope == null || scope.isBlank()) {
            return;
        }

        String cleanScope = scope.trim();
        authorityNames.add(cleanScope);

        String normalizedScope = cleanScope
                .replace(":", "_")
                .replace("-", "_")
                .replace(".", "_")
                .toUpperCase();

        authorityNames.add(normalizedScope);
    }

    private void addCandidatePermissions(String role, Set<String> authorityNames) {
        String cleanRole = role.replace("ROLE_", "").toUpperCase();

        if ("ADMIN".equals(cleanRole)
                || "RECRUITER".equals(cleanRole)
                || "TA".equals(cleanRole)
                || "TA_MANAGER".equals(cleanRole)
                || "TALENT_ACQUISITION".equals(cleanRole)) {

            authorityNames.add("CANDIDATE_CREATE");
            authorityNames.add("CANDIDATE_VIEW");
            authorityNames.add("CANDIDATE_UPDATE");
            authorityNames.add("CANDIDATE_DELETE");
        }

        if ("HIRING_MANAGER".equals(cleanRole)
                || "INTERVIEWER".equals(cleanRole)) {

            authorityNames.add("CANDIDATE_VIEW");
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }

    private void sendError(HttpServletResponse response, int status, String message)
            throws IOException {

        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }
}