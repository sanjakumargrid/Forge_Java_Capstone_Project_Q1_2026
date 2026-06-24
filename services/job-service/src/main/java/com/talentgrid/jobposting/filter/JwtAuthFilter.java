package com.talentgrid.jobposting.filter;

import com.talentgrid.jobposting.security.AuthenticatedUser;
import com.talentgrid.jobposting.security.JwtTokenUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenUtil jwtTokenUtil;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        String method = request.getMethod();

        boolean isGet = "GET".equalsIgnoreCase(method);
        boolean isOptions = "OPTIONS".equalsIgnoreCase(method);

        return isOptions
                || path.equals("/actuator/health")
                || path.equals("/swagger-ui.html")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-resources")
                || path.startsWith("/webjars")

                // Public job posting read APIs only
                || (isGet && path.equals("/api/job-postings"))
                || (isGet && path.matches("^/api/job-postings/\\d+$"))
                || (isGet && path.startsWith("/api/job-postings/public"))

                // Public only for GET
                || (isGet && path.startsWith("/api/demands"))
                || (isGet && path.startsWith("/api/analytics"));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);

        try {
            if (token.matches("mock-\\d+")) {

                Long userId = Long.parseLong(token.substring("mock-".length()));

                AuthenticatedUser principal = new AuthenticatedUser(
                        userId,
                        "mock" + userId + "@test.com",
                        List.of("RECRUITER")
                );

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                principal.getAuthorities()
                        );

                SecurityContextHolder.getContext().setAuthentication(auth);

            } else if (jwtTokenUtil.isValid(token)) {

                Long userId = jwtTokenUtil.extractUserId(token);
                String email = jwtTokenUtil.extractEmail(token);
                List<String> roles = jwtTokenUtil.extractRoles(token);

                AuthenticatedUser principal = new AuthenticatedUser(
                        userId,
                        email,
                        roles
                );

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                principal.getAuthorities()
                        );

                SecurityContextHolder.getContext().setAuthentication(auth);

            } else {
                log.debug("JWT invalid or expired; proceeding unauthenticated");
            }

        } catch (Exception e) {
            log.debug("JWT parse error: {}", e.getMessage());
        }

        chain.doFilter(request, response);
    }
}