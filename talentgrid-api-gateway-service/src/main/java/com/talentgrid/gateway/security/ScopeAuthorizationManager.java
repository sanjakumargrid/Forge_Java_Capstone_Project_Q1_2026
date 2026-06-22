package com.talentgrid.gateway.security;

import com.talentgrid.gateway.config.RbacProperties;
import com.talentgrid.gateway.constants.HeaderConstants;
import com.talentgrid.gateway.exception.GatewayErrorCodes;
import com.talentgrid.gateway.exception.GatewayErrorWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Global filter responsible for enforcing Role-Based Access Control (RBAC) via scopes and roles.
 * 
 * <p>This filter reads the {@link RbacProperties} loaded from the externalized {@code rbac-rules.yml}.
 * It compares the incoming request's path and HTTP method against the configured rules. If a matching
 * rule is found, it extracts the authenticated user's scopes and roles from the {@code X-User-Scopes} 
 * and {@code X-User-Roles} headers (injected previously by {@code JwtAuthenticationFilter}), and 
 * verifies if the user holds the necessary permissions.</p>
 * 
 * <p>This implements an explicit allow-list model: if a path is not defined as public and has no
 * matching RBAC rule, access is denied by default.</p>
 */
@Component
public class ScopeAuthorizationManager implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(ScopeAuthorizationManager.class);
    private static final AntPathMatcher pathMatcher = new AntPathMatcher();

    private final RbacProperties rbacProperties;
    private final GatewayErrorWriter gatewayErrorWriter;

    /**
     * Constructs a new {@code ScopeAuthorizationManager}.
     *
     * @param rbacProperties     configuration containing the active RBAC rules
     * @param gatewayErrorWriter utility for writing standardized JSON error responses
     */
    public ScopeAuthorizationManager(RbacProperties rbacProperties, GatewayErrorWriter gatewayErrorWriter) {
        this.rbacProperties = rbacProperties;
        this.gatewayErrorWriter = gatewayErrorWriter;
    }

    /**
     * Evaluates the request against the configured RBAC rules and either allows the request 
     * or returns a 403 Forbidden response.
     *
     * @param exchange the current server web exchange
     * @param chain    the gateway filter chain
     * @return a {@link Mono} that indicates when request processing is complete
     */
    @Override
    @SuppressWarnings("null")
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        String method = exchange.getRequest().getMethod().name();

        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        String scopesHeader = exchange.getRequest().getHeaders().getFirst(HeaderConstants.USER_SCOPES);
        String rolesHeader = exchange.getRequest().getHeaders().getFirst(HeaderConstants.USER_ROLES);

        Set<String> userScopes = splitTrimmed(scopesHeader);
        Set<String> userRoles = splitTrimmed(rolesHeader);

        Optional<RbacProperties.RbacRule> matchingRule = rbacProperties.getRules().stream()
                .filter(rule -> rule.getPath() != null && pathMatcher.match(rule.getPath(), path))
                .filter(rule -> rule.getMethods() == null || rule.getMethods().isEmpty()
                        || rule.getMethods().stream()
                                .filter(m -> m != null && !m.isBlank())
                                .map(String::trim)
                                .anyMatch(m -> m.equalsIgnoreCase(method)))
                .max(Comparator.comparingInt(r -> r.getPath().length()));

        if (matchingRule.isEmpty()) {
            log.warn("No RBAC rule defined for path: {} method: {} — denying access", path, method);
            return gatewayErrorWriter.write(exchange, HttpStatus.FORBIDDEN,
                    "No access rule defined for this resource and method",
                    GatewayErrorCodes.NO_RBAC_RULE);
        }

        Set<String> allowedScopes = normalizeList(matchingRule.get().getAllowedScopes());
        Set<String> allowedRoles = normalizeList(matchingRule.get().getAllowedRoles());

        boolean hasScopeAccess = allowedScopes.isEmpty() || !Collections.disjoint(userScopes, allowedScopes);
        boolean hasRoleAccess = allowedRoles.isEmpty() || !Collections.disjoint(userRoles, allowedRoles);

        if (!hasScopeAccess && !hasRoleAccess) {
            log.warn("Access denied: scopes={} roles={} path={} method={} (allowed scopes: {}, allowed roles: {})",
                    userScopes, userRoles, path, method, allowedScopes, allowedRoles);
            return gatewayErrorWriter.write(exchange, HttpStatus.FORBIDDEN,
                    "User is not authorized to perform this action on this resource",
                    GatewayErrorCodes.FORBIDDEN);
        }

        log.debug("Access granted: scopes={} path={} method={}", userScopes, path, method);
        return chain.filter(exchange);
    }

    private static Set<String> splitTrimmed(String header) {
        if (header == null || header.isBlank()) {
            return Collections.emptySet();
        }
        return Arrays.stream(header.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    private static Set<String> normalizeList(List<String> list) {
        if (list == null || list.isEmpty()) {
            return Collections.emptySet();
        }
        return list.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    @SuppressWarnings("null")
    private boolean isPublicPath(String path) {
        return rbacProperties.getPublicPaths().stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    /**
     * Determines the execution order of this filter.
     * 
     * @return the order value (-1), ensuring it runs immediately after JwtAuthenticationFilter
     */
    @Override
    public int getOrder() {
        return -1;
    }
}
