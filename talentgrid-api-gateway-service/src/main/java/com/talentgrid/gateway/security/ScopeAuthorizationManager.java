package com.talentgrid.gateway.security;

import com.talentgrid.gateway.config.RbacProperties;
import com.talentgrid.gateway.constants.HeaderConstants;
import com.talentgrid.gateway.util.TraceUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class ScopeAuthorizationManager implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(ScopeAuthorizationManager.class);
    private static final AntPathMatcher pathMatcher = new AntPathMatcher();

    private final RbacProperties rbacProperties;
    private final ObjectMapper objectMapper;
    private final TraceUtil traceUtil;

    public ScopeAuthorizationManager(RbacProperties rbacProperties, ObjectMapper objectMapper, TraceUtil traceUtil) {
        this.rbacProperties = rbacProperties;
        this.objectMapper = objectMapper;
        this.traceUtil = traceUtil;
    }

    @Override
    @SuppressWarnings("null")
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        String method = exchange.getRequest().getMethod().name();

        // Public paths already passed JwtAuthenticationFilter — no role check needed
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        // By this point JwtAuthenticationFilter has validated the token and set
        // X-User-Scopes
        String scopesHeader = exchange.getRequest().getHeaders().getFirst(HeaderConstants.USER_SCOPES);
        String rolesHeader = exchange.getRequest().getHeaders().getFirst(HeaderConstants.USER_ROLES);

        List<String> userScopes = scopesHeader != null && !scopesHeader.isEmpty()
                ? Arrays.asList(scopesHeader.split(","))
                : Collections.emptyList();

        List<String> userRoles = rolesHeader != null && !rolesHeader.isEmpty()
                ? Arrays.asList(rolesHeader.split(","))
                : Collections.emptyList();

        // Find matching rule by Path AND Method
        Optional<RbacProperties.RbacRule> matchingRule = rbacProperties.getRules().stream()
                .filter(rule -> pathMatcher.match(rule.getPath(), path))
                .filter(rule -> rule.getMethods() == null || rule.getMethods().isEmpty()
                        || rule.getMethods().contains(method))
                .findFirst();

        if (matchingRule.isEmpty()) {
            log.warn("No RBAC rule defined for path: {} method: {} — denying access", path, method);
            return writeError(exchange, HttpStatus.FORBIDDEN, "No access rule defined for this resource and method");
        }

        List<String> allowedScopes = matchingRule.get().getAllowedScopes();
        List<String> allowedRoles = matchingRule.get().getAllowedRoles();

        boolean hasScopeAccess = allowedScopes == null || allowedScopes.isEmpty()
                || !Collections.disjoint(userScopes, allowedScopes);
        boolean hasRoleAccess = allowedRoles == null || allowedRoles.isEmpty()
                || !Collections.disjoint(userRoles, allowedRoles);

        // Deny only when the user has NEITHER the required scope NOR the required role.
        // If a rule defines only allowed-scopes, hasRoleAccess is automatically true (null check).
        // If a rule defines both, the user needs at least one of either to pass.
        if (!hasScopeAccess && !hasRoleAccess) {
            log.warn("Access denied: scopes={} roles={} path={} method={} (allowed scopes: {}, allowed roles: {})",
                    userScopes, userRoles, path, method, allowedScopes, allowedRoles);
            return writeError(exchange, HttpStatus.FORBIDDEN,
                    "User is not authorized to perform this action on this resource");
        }

        log.debug("Access granted: scopes={} path={} method={}", userScopes, path, method);
        return chain.filter(exchange);
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
        return -1; // runs after JwtAuthenticationFilter (order -2)
    }
}
