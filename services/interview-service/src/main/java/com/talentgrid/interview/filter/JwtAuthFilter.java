package com.talentgrid.interview.filter;

import com.talentgrid.shared.auth.security.JwtAuthenticationProvider;
import com.talentgrid.shared.auth.security.JwtPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
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

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String method = request.getMethod();
        String servletPath = request.getServletPath();
        String requestUri = request.getRequestURI();

        boolean isPublicEndpoint =
                "OPTIONS".equalsIgnoreCase(method)
                        || servletPath.startsWith("/actuator")
                        || servletPath.startsWith("/swagger-ui")
                        || servletPath.startsWith("/v3/api-docs")
                        || "/swagger-ui.html".equals(servletPath)

                        // Google OAuth endpoints
                        || servletPath.startsWith("/api/google/oauth")
                        || servletPath.startsWith("/api/v1/google/oauth")
                        || servletPath.startsWith("/google/oauth")
                        || requestUri.contains("/api/google/oauth")
                        || requestUri.contains("/api/v1/google/oauth")
                        || requestUri.contains("/google/oauth");

        if (isPublicEndpoint) {
            log.info("Skipping JwtAuthFilter for public endpoint | method={} | servletPath={} | requestUri={}",
                    method, servletPath, requestUri);
        }

        return isPublicEndpoint;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            JwtPrincipal principal = jwtAuthenticationProvider.authenticate(token);

            Set<String> authorityNames = new LinkedHashSet<>();

            if (principal.getScopes() != null) {
                principal.getScopes().forEach(scope -> {
                    if (scope != null && !scope.isBlank()) {
                        authorityNames.add(scope.trim().toUpperCase());
                    }
                });
            }

            if (principal.getRoles() != null) {
                principal.getRoles().forEach(role -> {
                    if (role != null && !role.isBlank()) {
                        String cleanRole = role.trim().toUpperCase().replace("ROLE_", "");
                        authorityNames.add(cleanRole);
                        authorityNames.add("ROLE_" + cleanRole);

                        addInterviewPermissions(cleanRole, authorityNames);
                    }
                });
            }

            var authorities = authorityNames.stream()
                    .map(SimpleGrantedAuthority::new)
                    .toList();

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(principal, null, authorities);

            SecurityContextHolder.getContext().setAuthentication(auth);

            log.info("Authenticated interview-service userId={}, email={}, authorities={}",
                    principal.getUserId(),
                    principal.getEmail(),
                    authorityNames
            );

        } catch (Exception e) {
            log.warn("JWT validation failed: {}", e.getMessage());

            SecurityContextHolder.clearContext();

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Invalid or expired token\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void addInterviewPermissions(String role, Set<String> authorityNames) {
        if ("ADMIN".equals(role)
                || "RECRUITER".equals(role)
                || "TA".equals(role)
                || "TALENT_ACQUISITION".equals(role)
                || "HIRING_MANAGER".equals(role)) {

            authorityNames.add("INTERVIEW_CREATE");
            authorityNames.add("INTERVIEW_VIEW");
            authorityNames.add("INTERVIEW_UPDATE");
            authorityNames.add("INTERVIEW_DELETE");
            authorityNames.add("INTERVIEW_SCHEDULE");
            authorityNames.add("INTERVIEW_RESCHEDULE");
            authorityNames.add("INTERVIEW_CANCEL");
        }

        if ("INTERVIEWER".equals(role)) {
            authorityNames.add("INTERVIEW_VIEW");
            authorityNames.add("INTERVIEW_UPDATE");
        }
    }
}