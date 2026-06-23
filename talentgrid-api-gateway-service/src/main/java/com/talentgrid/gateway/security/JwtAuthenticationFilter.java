package com.talentgrid.gateway.security;

import com.talentgrid.gateway.config.RbacProperties;
import com.talentgrid.gateway.constants.HeaderConstants;
import com.talentgrid.gateway.exception.GatewayErrorCodes;
import com.talentgrid.gateway.exception.GatewayErrorWriter;
import com.talentgrid.gateway.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

/**
 * Global filter responsible for stateless JWT authentication and identity propagation.
 * 
 * <p>This filter acts as the primary security barrier for the API Gateway. It extracts the
 * {@code Authorization: Bearer} token, verifies its signature and claims, checks the token
 * against a Redis-backed blacklist to enforce revocation, and finally injects trusted identity
 * headers (such as {@code X-User-Id}, {@code X-User-Roles}) into the downstream request.</p>
 * 
 * <p>It also sanitizes incoming requests by stripping any existing identity headers to prevent
 * spoofing attacks. Public paths (defined in {@link RbacProperties}) bypass token validation
 * entirely.</p>
 * 
 * <p>Implements {@link GlobalFilter} and {@link Ordered} to execute before authorization
 * but after basic correlation and request validation.</p>
 */
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final AntPathMatcher pathMatcher = new AntPathMatcher();

    private final JwtUtil jwtUtil;
    private final RbacProperties rbacProperties;
    private final GatewayErrorWriter gatewayErrorWriter;
    private final JwtBlacklistService jwtBlacklistService;

    /**
     * Constructs a new {@code JwtAuthenticationFilter}.
     *
     * @param jwtUtil             utility for parsing and validating JWTs
     * @param rbacProperties      configuration containing public paths and RBAC rules
     * @param gatewayErrorWriter  utility for writing standardized JSON error responses
     * @param jwtBlacklistService service to verify token revocation status
     */
    public JwtAuthenticationFilter(JwtUtil jwtUtil, RbacProperties rbacProperties,
            GatewayErrorWriter gatewayErrorWriter, JwtBlacklistService jwtBlacklistService) {
        this.jwtUtil = jwtUtil;
        this.rbacProperties = rbacProperties;
        this.gatewayErrorWriter = gatewayErrorWriter;
        this.jwtBlacklistService = jwtBlacklistService;
    }

    /**
     * intercepts the request to enforce authentication and propagate identity.
     *
     * @param exchange the current server web exchange
     * @param chain    the gateway filter chain
     * @return a {@link Mono} that indicates when request processing is complete
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        ServerHttpRequest cleanRequest = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove(HeaderConstants.USER_ROLES);
                    headers.remove(HeaderConstants.USER_SCOPES);
                    headers.remove(HeaderConstants.USER_EMAIL);
                    headers.remove(HeaderConstants.AUTH_TIME);
                    if (!isPublicPath(path)) {
                        headers.remove(HeaderConstants.USER_ID);
                    }
                })
                .build();

        ServerWebExchange cleanExchange = exchange.mutate().request(cleanRequest).build();

        if (isPublicPath(path)) {
            return chain.filter(cleanExchange);
        }

        String authHeader = cleanExchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        String token = (authHeader != null && authHeader.startsWith("Bearer ")) ? authHeader.substring(7) : null;

        if (token == null) {
            log.warn("Missing or malformed Authorization header for path: {}", path);
            return gatewayErrorWriter.write(cleanExchange, HttpStatus.UNAUTHORIZED,
                    "Missing or invalid Authorization header", GatewayErrorCodes.UNAUTHORIZED);
        }
        try {
            Claims claims = jwtUtil.validateAndParse(token);

            String jti = jwtUtil.extractJti(claims);

            return jwtBlacklistService.isBlacklisted(jti).flatMap(isBlacklisted -> {
                if (Boolean.TRUE.equals(isBlacklisted)) {
                    log.warn("Attempt to use blacklisted token (jti: {}) for path: {}", jti, path);
                    return gatewayErrorWriter.write(cleanExchange, HttpStatus.UNAUTHORIZED,
                            "Token has been invalidated", GatewayErrorCodes.UNAUTHORIZED);
                }

                String userId = jwtUtil.extractUserId(claims);
                List<String> roles = jwtUtil.extractRoles(claims);
                List<String> scopes = jwtUtil.extractScopes(claims);
                String email = jwtUtil.extractEmail(claims);

                String rolesHeader = String.join(",", roles);
                String scopesHeader = String.join(",", scopes);

                ServerHttpRequest.Builder requestBuilder = cleanExchange.getRequest().mutate()
                        .header(HeaderConstants.USER_ID, userId)
                        .header(HeaderConstants.USER_ROLES, rolesHeader)
                        .header(HeaderConstants.USER_SCOPES, scopesHeader)
                        .header(HeaderConstants.AUTH_TIME, Instant.now().toString());

                if (email != null && !email.isEmpty()) {
                    requestBuilder.header(HeaderConstants.USER_EMAIL, email);
                }

                ServerHttpRequest mutatedRequest = requestBuilder.build();
                return chain.filter(cleanExchange.mutate().request(mutatedRequest).build());
            });

        } catch (JwtException e) {
            log.warn("JWT validation failed for path {}: {}", path, e.getMessage());
            return gatewayErrorWriter.write(cleanExchange, HttpStatus.UNAUTHORIZED,
                    "Invalid or expired token", GatewayErrorCodes.UNAUTHORIZED);
        }
    }

    @SuppressWarnings("null")
    private boolean isPublicPath(String path) {
        return rbacProperties.getPublicPaths().stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    /**
     * Determines the execution order of this filter.
     * 
     * @return the order value (-2), ensuring it runs before {@link ScopeAuthorizationManager} (-1)
     */
    @Override
    public int getOrder() {
        return -2;
    }
}
