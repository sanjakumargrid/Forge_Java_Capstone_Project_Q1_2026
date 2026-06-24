package com.talentgrid.auth.filter;

import com.talentgrid.shared.auth.dto.CachedUserContext;
import com.talentgrid.auth.jwt.JwtBlacklistService;
import com.talentgrid.auth.jwt.JwtService;
import com.talentgrid.auth.security.CachedUserPrincipal;
import com.talentgrid.auth.service.interfaces.AuthorizationValidationService;
import com.talentgrid.auth.service.interfaces.UserSecurityCacheService;
import com.talentgrid.shared.auth.constants.JwtConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Spring Security filter that intercepts HTTP requests, validates JWT access tokens,
 * and populates the SecurityContext.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Extract JWT from the Authorization header</li>
 *   <li>Verify signature and expiration via {@link JwtService}</li>
 *   <li>Check against Redis blacklist (logout validation)</li>
 *   <li>Validate token type is 'access'</li>
 *   <li>Check {@code authVersion} against Redis to handle real-time authorization invalidation</li>
 *   <li>Ensure the user's account is still enabled</li>
 *   <li>Construct a full authenticated Spring Security token</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    private final JwtBlacklistService jwtBlacklistService;

    private final AuthorizationValidationService authorizationValidationService;

    private final UserSecurityCacheService userSecurityCacheService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getServletPath();

        // PUBLIC ENDPOINTS
        if (path.equals("/api/v1/auth/login")
                || path.equals("/api/v1/auth/register")
                || path.equals("/api/v1/auth/refresh")
                || path.equals("/api/v1/auth/oauth/token")
                || path.startsWith("/oauth2/")
                || path.startsWith("/login/oauth2/")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
        ) {

            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader =
                request.getHeader("Authorization");

        // NO JWT HEADER
        if (authHeader == null
                || !authHeader.startsWith("Bearer ")) {

            filterChain.doFilter(request, response);
            return;
        }

        String jwtToken =
                authHeader.substring(7);

        try {
            // ==========================================
            // EXTRACT & VALIDATE CLAIMS
            // ==========================================
            String jti = jwtService.extractJti(jwtToken);
            String tokenType = jwtService.extractTokenType(jwtToken);
            
            if (!JwtConstants.ACCESS_TOKEN.equals(tokenType)) {
                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid token type");
                return;
            }

            if (jwtBlacklistService.isBlacklisted(jti)) {
                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "You are already logged out");
                return;
            }

            Long userId = Long.valueOf(jwtService.extractUserId(jwtToken));
            Long jwtVersion = jwtService.extractAuthVersion(jwtToken);
            CachedUserContext cachedUser = userSecurityCacheService.getUser(userId);

            if (cachedUser == null) {
                // Cache miss - require re-login rather than silently falling through
                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Session expired. Please login again.");
                return;
            }
            
            if (!Boolean.TRUE.equals(cachedUser.getEnabled())) {
                sendErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, "Account has been disabled.");
                return;
            }

            if (!cachedUser.getAuthVersion().equals(jwtVersion)) {
                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Authorization changed. Please login again.");
                return;
            }

            String userEmail = jwtService.extractEmail(jwtToken);

            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                Set<SimpleGrantedAuthority> authorities = new HashSet<>();

                cachedUser.getRoles().forEach(role ->
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));

                cachedUser.getScopes().forEach(scope ->
                        authorities.add(new SimpleGrantedAuthority(scope)));

                CachedUserPrincipal principal = CachedUserPrincipal.builder()
                        .userId(cachedUser.getUserId())
                        .email(cachedUser.getEmail())
                        .enabled(cachedUser.getEnabled())
                        .authVersion(cachedUser.getAuthVersion())
                        .build();

                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                authorities
                        );

                authToken.setDetails(
                        new WebAuthenticationDetailsSource()
                                .buildDetails(request)
                );

                SecurityContextHolder.getContext().setAuthentication(authToken);
            }

        } catch (Exception e) {
            log.warn("JWT Authentication failed: {}", e.getMessage());
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired JWT");
            return;
        }

        filterChain.doFilter(request, response);
    }
    
    /**
     * Utility to write JSON error responses.
     */
    private void sendErrorResponse(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write(String.format("{\"error\": \"%s\"}", message));
    }
}