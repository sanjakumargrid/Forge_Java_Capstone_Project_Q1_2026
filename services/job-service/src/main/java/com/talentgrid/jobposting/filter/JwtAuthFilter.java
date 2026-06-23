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
                // Frontend dev/demo login (libs/auth/src/lib/auth.service.ts) issues
                // `mock-<userId>` tokens with no embedded role — there's no real JWT
                // to validate, so synthesize a principal directly from the id. Role is
                // a placeholder; this service has no @PreAuthorize checks that read it.
                Long userId = Long.parseLong(token.substring("mock-".length()));
                AuthenticatedUser principal = new AuthenticatedUser(
                        userId, "mock" + userId + "@test.com", List.of("RECRUITER"));
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

                SecurityContextHolder.getContext().setAuthentication(auth);
            } else if (jwtTokenUtil.isValid(token)) {
                Long userId = jwtTokenUtil.extractUserId(token);
                String email = jwtTokenUtil.extractEmail(token);
                List<String> roles = jwtTokenUtil.extractRoles(token);

                AuthenticatedUser principal = new AuthenticatedUser(userId, email, roles);
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

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
