package offerService.filter;

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
        String path = request.getServletPath();

        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                || path.startsWith("/actuator")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || "/swagger-ui.html".equals(path);
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
                principal.getScopes().forEach(scope -> addAuthority(scope, authorityNames));
            }

            if (principal.getRoles() != null) {
                principal.getRoles().forEach(role -> {
                    addAuthority(role, authorityNames);

                    String cleanRole = normalize(role).replace("ROLE_", "");
                    authorityNames.add(cleanRole);
                    authorityNames.add("ROLE_" + cleanRole);

                    addOfferPermissions(cleanRole, authorityNames);
                });
            }

            var authorities = authorityNames.stream()
                    .map(SimpleGrantedAuthority::new)
                    .toList();

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, authorities);

            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.info("Authenticated offer-service userId={}, email={}, authorities={}",
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

    private void addOfferPermissions(String role, Set<String> authorityNames) {
        String cleanRole = role.replace("ROLE_", "").toUpperCase();

        if ("ADMIN".equals(cleanRole)
                || "RECRUITER".equals(cleanRole)
                || "TA".equals(cleanRole)
                || "TALENT_ACQUISITION".equals(cleanRole)) {

            authorityNames.add("OFFER_CREATE");
            authorityNames.add("OFFER_VIEW");
            authorityNames.add("OFFER_UPDATE");
            authorityNames.add("OFFER_DELETE");
            authorityNames.add("OFFER_SEND");
            authorityNames.add("OFFER_APPROVE");
            authorityNames.add("OFFER_SIGN");
        }

        if ("HIRING_MANAGER".equals(cleanRole)) {
            authorityNames.add("OFFER_VIEW");
            authorityNames.add("OFFER_APPROVE");
        }

        if ("CANDIDATE".equals(cleanRole)) {
            authorityNames.add("OFFER_VIEW");
            authorityNames.add("OFFER_SIGN");
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }
}