package com.talentgrid.gateway.security;

import com.talentgrid.gateway.config.RbacProperties;
import com.talentgrid.gateway.constants.HeaderConstants;
import com.talentgrid.gateway.util.TraceUtil;
import com.talentgrid.gateway.util.JwtUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final AntPathMatcher pathMatcher = new AntPathMatcher();

    private final JwtUtil jwtUtil;
    private final RbacProperties rbacProperties;
    private final ObjectMapper objectMapper;
    private final TraceUtil traceUtil;
    private final JwtBlacklistService jwtBlacklistService;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, RbacProperties rbacProperties, ObjectMapper objectMapper,
            TraceUtil traceUtil, JwtBlacklistService jwtBlacklistService) {
        this.jwtUtil = jwtUtil;
        this.rbacProperties = rbacProperties;
        this.objectMapper = objectMapper;
        this.traceUtil = traceUtil;
        this.jwtBlacklistService = jwtBlacklistService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        // Strip any existing security headers to prevent spoofing
        ServerHttpRequest cleanRequest = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove(HeaderConstants.USER_ID);
                    headers.remove(HeaderConstants.USER_ROLES);
                    headers.remove(HeaderConstants.USER_SCOPES);
                    headers.remove(HeaderConstants.USER_EMAIL);
                    headers.remove(HeaderConstants.AUTH_TIME);
                })
                .build();

        ServerWebExchange cleanExchange = exchange.mutate().request(cleanRequest).build();

        // Skip auth for public paths defined in rbac-rules.yml
        if (isPublicPath(path)) {
            return chain.filter(cleanExchange);
        }

        String authHeader = cleanExchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        String token = (authHeader != null && authHeader.startsWith("Bearer ")) ? authHeader.substring(7) : null;

        if (token == null) {
            log.warn("Missing or malformed Authorization header for path: {}", path);
            return writeError(cleanExchange, HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");
        }
        try {
            Claims claims = jwtUtil.validateAndParse(token);

            String jti = jwtUtil.extractJti(claims);

            return jwtBlacklistService.isBlacklisted(jti).flatMap(isBlacklisted -> {
                if (Boolean.TRUE.equals(isBlacklisted)) {
                    log.warn("Attempt to use blacklisted token (jti: {}) for path: {}", jti, path);
                    return writeError(cleanExchange, HttpStatus.UNAUTHORIZED, "Token has been invalidated");
                }

                String userId = jwtUtil.extractUserId(claims);
                List<String> roles = jwtUtil.extractRoles(claims);
                List<String> scopes = jwtUtil.extractScopes(claims);
                String email = jwtUtil.extractEmail(claims);

                String rolesHeader = roles != null ? String.join(",", roles) : "";
                String scopesHeader = scopes != null ? String.join(",", scopes) : "";

                // Forward identity downstream as trusted headers (internal cluster only)
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
            return writeError(cleanExchange, HttpStatus.UNAUTHORIZED, "Invalid or expired token");
        }
    }

    @SuppressWarnings("null")
    private boolean isPublicPath(String path) {
        return rbacProperties.getPublicPaths().stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    @SuppressWarnings("null")
    private Mono<Void> writeError(ServerWebExchange exchange, HttpStatus status, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String traceId = traceUtil.getCurrentTraceId();

        Map<String, Object> errorDetails = Map.of(
                "timestamp", Instant.now().toString(),
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", message,
                "path", exchange.getRequest().getPath().value(),
                "traceId", traceId != null ? traceId : "unknown");

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(errorDetails);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize error response", e);
            bytes = "{\"error\":\"Internal Server Error\"}".getBytes(StandardCharsets.UTF_8);
        }

        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -2; // runs before ScopeAuthorizationManager (order -1)
    }
}
