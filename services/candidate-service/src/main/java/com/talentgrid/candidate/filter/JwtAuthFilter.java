package com.talentgrid.candidate.filter;

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
        String path = request.getServletPath();
        String method = request.getMethod();

        boolean publicCandidatePost =
                "POST".equalsIgnoreCase(method)
                        && (
                        "/api/v1/external-candidates".equals(path)
                                || "/api/v1/external-candidates/".equals(path)
                );

        boolean internalCandidateGet =
                "GET".equalsIgnoreCase(method)
                        && path.startsWith("/api/v1/external-candidates/internal/");

        return publicCandidatePost || internalCandidateGet;
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

            String redisKey = "auth:user:" + principal.getUserId();
            Boolean exists = objectRedisTemplate.hasKey(redisKey);

            if (!Boolean.TRUE.equals(exists)) {
                SecurityContextHolder.clearContext();
                sendError(response, HttpServletResponse.SC_UNAUTHORIZED,
                        "Authorization changed. Please login again.");
                return;
            }

            Set<String> authorityNames = new LinkedHashSet<>();

            if (principal.getScopes() != null) {
                principal.getScopes().forEach(scope -> addAuthority(scope, authorityNames));
            }

            if (principal.getRoles() != null) {
                principal.getRoles().forEach(role -> {
                    addAuthority(role, authorityNames);

                    String cleanRole = normalize(role).replace("ROLE_", "");
                    authorityNames.add("ROLE_" + cleanRole);

                    addCandidatePermissions(cleanRole, authorityNames);
                });
            }

            var authorities = authorityNames.stream()
                    .map(SimpleGrantedAuthority::new)
                    .toList();

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, authorities);

            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.info("Authenticated candidate-service userId={}, email={}, authorities={}",
                    principal.getUserId(),
                    principal.getEmail(),
                    authorityNames
            );

        } catch (Exception e) {
            log.warn("JWT validation failed: {}", e.getMessage());

            SecurityContextHolder.clearContext();

            sendError(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Invalid or expired token");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void addAuthority(String value, Set<String> authorityNames) {
        if (value == null || value.isBlank()) {
            return;
        }

        String cleanValue = normalize(value);
        authorityNames.add(cleanValue);

        String permission = cleanValue
                .replace(":", "_")
                .replace("-", "_")
                .replace(".", "_")
                .toUpperCase();

        authorityNames.add(permission);
    }

    private void addCandidatePermissions(String role, Set<String> authorityNames) {
        String cleanRole = role.replace("ROLE_", "").toUpperCase();

        if ("ADMIN".equals(cleanRole)
                || "RECRUITER".equals(cleanRole)
                || "TA".equals(cleanRole)
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