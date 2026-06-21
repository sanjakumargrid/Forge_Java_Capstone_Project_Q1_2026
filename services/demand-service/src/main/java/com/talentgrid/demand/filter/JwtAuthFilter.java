package com.talentgrid.demand.filter;

import com.talentgrid.shared.auth.security.JwtAuthenticationProvider;
import com.talentgrid.shared.auth.security.JwtPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {


    private final JwtAuthenticationProvider jwtAuthenticationProvider;
    private final RedisTemplate<String, Object> objectRedisTemplate;

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
            // Step 1 - Validate JWT signature, expiry, token type
            JwtPrincipal principal = jwtAuthenticationProvider.authenticate(token);

            // Step 2 - Check Redis cache (evicted by Kafka consumer when admin changes something)
            String redisKey = "auth:user:" + principal.getUserId();
            Boolean exists = objectRedisTemplate.hasKey(redisKey);
            if (!Boolean.TRUE.equals(exists)) {
                // Cache was evicted — admin changed something — force re-login
                sendError(response, HttpServletResponse.SC_UNAUTHORIZED,
                        "Authorization changed. Please login again.");
                return;
            }

            // Step 3 - Set security context
            List<SimpleGrantedAuthority> authorities = principal.getScopes()
                    .stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(principal, null, authorities);

            SecurityContextHolder.getContext().setAuthentication(auth);

        } catch (Exception e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Invalid or expired token");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void sendError(HttpServletResponse response, int status, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\": \"" + message + "\"}");
    }

}