# Integrating `talentgrid-shared` with a Microservice

This guide documents the steps to integrate the `talentgrid-shared` JWT library into any TalentGrid microservice so it can validate JWT tokens issued by the `user-auth-service`.

---

## Prerequisites

- The microservice is part of the `Forge_Java_Capstone_Project_Q1_2026_Services` multi-module Maven project
- `user-auth-service` is running and issuing tokens
- Both services share the **same `JWT_SECRET`** value

---

## Understanding the Architecture

```
user-auth-service                    any-microservice
      │                                     │
      │  signs JWT with                     │  verifies JWT with
      │  HMAC-HS256                         │  HMAC-HS256
      │  using JWT_SECRET                   │  using same JWT_SECRET
      └──────────────── token ─────────────►│
                                            │
                                    talentgrid-shared
                                    (validation logic)
```

`talentgrid-shared` owns all JWT parsing and validation logic. Each microservice only needs to:
1. Add it as a dependency
2. Wire a filter to apply it to incoming HTTP requests
3. Share the same secret key

---

## Why Each Component is Needed

| Component | Where | Why |
|---|---|---|
| `JwtTokenService` | talentgrid-shared | Parses and verifies JWT signature |
| `JwtAuthenticationProvider` | talentgrid-shared | Builds `JwtPrincipal` from validated token |
| `JwtPrincipal` | talentgrid-shared | Holds userId, email, roles, scopes |
| `JwtAutoConfiguration` | talentgrid-shared | Registers `JwtTokenService` as a Spring bean |
| `AutoConfiguration.imports` | talentgrid-shared/resources | Tells Spring Boot to load `JwtAutoConfiguration` |
| `JwtConfig` | each service | Wires `JwtAuthenticationProvider` into service context |
| `JwtAuthFilter` | each service | Applies JWT validation to HTTP requests |
| `SecurityConfig` | each service | Controls which endpoints require auth |

---

## Step 1 — Verify talentgrid-shared has the AutoConfiguration registration file

This file is required for Spring Boot to discover `JwtAutoConfiguration` automatically. Without it, `JwtTokenService` bean will not be created.

Verify the file exists at:
```
talentgrid-shared/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

It must contain exactly:
```
com.talentgrid.shared.config.JwtAutoConfiguration
```

If it does not exist, create it:
```bash
mkdir -p talentgrid-shared/src/main/resources/META-INF/spring

echo "com.talentgrid.shared.config.JwtAutoConfiguration" > \
  talentgrid-shared/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

> **Why:** Spring Boot 3.x does not scan JAR files for `@Configuration` classes automatically. It only loads them if explicitly listed in this file. Skipping this causes `No qualifying bean of type 'JwtTokenService'` on startup.

---

## Step 2 — Add talentgrid-shared dependency to the service's pom.xml

Open `services/<your-service>/pom.xml` and add:

```xml
<dependency>
    <groupId>com.talentgrid</groupId>
    <artifactId>talentgrid-shared</artifactId>
</dependency>
```

No version tag needed — it is managed by the root parent pom via `${project.version}`.

---

## Step 3 — Add JWT secret to the service's application.properties

Open `services/<your-service>/src/main/resources/application.properties` and add:

```properties
# Must match JWT_SECRET in user-auth-service .env exactly
jwt.secret=${JWT_SECRET}
```

The value after `:` is the default used when no environment variable is set (local development).

> **Critical:** The secret must be byte-for-byte identical to the one in `user-auth-service`. Any difference — including trailing spaces, newlines, or different casing — will cause `JWT signature does not match` errors.

---

## Step 4 — Create JwtConfig in the service

Create `src/main/java/com/talentgrid/<service>/config/JwtConfig.java`:

```java
package com.talentgrid.<service>.config;

import com.talentgrid.shared.auth.jwt.JwtTokenService;
import com.talentgrid.shared.auth.security.JwtAuthenticationProvider;
import com.talentgrid.shared.config.JwtAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(JwtAutoConfiguration.class)
public class JwtConfig {

    @Bean
    public JwtAuthenticationProvider jwtAuthenticationProvider(JwtTokenService jwtTokenService) {
        return new JwtAuthenticationProvider(jwtTokenService);
    }
}
```

> **Why `@Import(JwtAutoConfiguration.class)`:** This explicitly pulls in the shared configuration regardless of Spring Boot's auto-configuration scanning. It is the most reliable way to ensure `JwtTokenService` is always available as a bean, even if the `AutoConfiguration.imports` file is not picked up due to classpath ordering or Spring Boot version differences.

---

## Step 5 — Create JwtAuthFilter in the service

Create `src/main/java/com/talentgrid/<service>/filter/JwtAuthFilter.java`:

```java
package com.talentgrid.<service>.filter;

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
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtAuthenticationProvider jwtAuthenticationProvider;

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

            List<SimpleGrantedAuthority> authorities = principal.getScopes()
                    .stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(principal, null, authorities);

            SecurityContextHolder.getContext().setAuthentication(auth);

        } catch (Exception e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Invalid or expired token\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
```

> **Why in each service and not in shared:** The filter is a Spring Security web component. Each service needs control over which endpoints it applies to and how it handles errors. The shared library intentionally avoids owning web-layer behaviour.

---

## Step 6 — Update SecurityConfig in the service

Replace the existing `SecurityConfig.java` with:

```java
package com.talentgrid.<service>.config;

import com.talentgrid.<service>.filter.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**", "/v3/api-docs/**", "/swagger-ui/**")
                .permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
```

---

## Step 7 — Update SecurityUtils in the service

Replace the existing `SecurityUtils.java` to read from `JwtPrincipal` set by the filter:

```java
package com.talentgrid.<service>.util;

import com.talentgrid.shared.auth.security.JwtPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {

    private SecurityUtils() {}

    public static String getCurrentUserEmail() {
        JwtPrincipal p = getPrincipal();
        return p != null ? p.getEmail() : "system@griddynamics.com";
    }

    public static Long getCurrentUserId() {
        JwtPrincipal p = getPrincipal();
        return p != null ? p.getUserId() : 1L;
    }

    private static JwtPrincipal getPrincipal() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof JwtPrincipal jwtPrincipal) {
            return jwtPrincipal;
        }
        return null;
    }
}
```

---

## Step 8 — Build in the correct order

Always install `talentgrid-shared` before building any service that depends on it:

```bash
# From project root

# Step 1 — clear stale cached artifacts
rm -rf ~/.m2/repository/com/talentgrid

# Step 2 — install shared library first
cd talentgrid-shared
mvn clean install -DskipTests

# Step 3 — build the full project
cd ..
mvn clean install -DskipTests
```

> **Why clear .m2:** Maven caches JARs locally. If an old empty version of `talentgrid-shared` is cached, your service will use that instead of the updated one even after rebuilding. Clearing the cache forces Maven to use the freshly built JAR.

---

## Step 9 — Add  relevant scopes for your service to the database

The JWT token embeds scopes at login time from the database. If your service uses `@PreAuthorize` checks, those scopes must exist in the DB and be assigned to the appropriate role.

```sql
-- Insert your service's scopes
INSERT INTO scopes (name) VALUES
('<YOUR_SERVICE>_VIEW'),
('<YOUR_SERVICE>_CREATE'),
('<YOUR_SERVICE>_UPDATE'),
('<YOUR_SERVICE>_DELETE')
ON CONFLICT (name) DO NOTHING;

-- Assign to ADMIN role
INSERT INTO role_scopes (role_id, scope_id)
SELECT r.id, s.id
FROM roles r, scopes s
WHERE r.name = 'ADMIN'
AND s.name IN ('<YOUR_SERVICE>_VIEW', '<YOUR_SERVICE>_CREATE', '<YOUR_SERVICE>_UPDATE', '<YOUR_SERVICE>_DELETE')
ON CONFLICT DO NOTHING;
```

After running the SQL, **login again** to get a fresh token with the new scopes embedded.

---

## Step 10 — Test the integration

```bash
# Get a fresh token
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"your@email.com","password":"yourpassword"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")

echo "TOKEN: $TOKEN"

# Call your service with the token
curl -s -X GET http://localhost:<your-service-port>/api/v1/<your-endpoint> \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json"
```

Expected results:
- `200 OK` — JWT valid, scopes match
- `401 Unauthorized` — JWT invalid, expired, or missing
- `403 Forbidden` — JWT valid but user lacks required scope (add scopes to DB and re-login)

---

## Common Errors and Solutions

| Error | Cause | Fix |
|---|---|---|
| `No qualifying bean of type 'JwtTokenService'` | `AutoConfiguration.imports` file missing or shared JAR not rebuilt | Create the imports file, clear `.m2`, rebuild shared |
| `JWT signature does not match` | Token expired (JJWT throws misleading error) or wrong secret | Get a fresh token; verify `jwt.secret` matches `JWT_SECRET` in auth service |
| `403 Forbidden` after JWT passes | User's role lacks required scopes | Insert scopes into DB, assign to role, re-login |
| `RS256 algorithm but SecretKeySpec provided` | Auth service signing with RSA but shared lib using HMAC | Ensure both services use the same algorithm — check `JwtService.getSigningKey()` |
| `JWT signature does not match` in Postman only | Token pasted with hidden newline character | Use Postman's **Authorization tab → Bearer Token** field instead of manual header |

---

## Key Rules to Remember

1. **Always rebuild `talentgrid-shared` before the consuming service** — stale JARs in `.m2` cause phantom errors
2. **The JWT secret must be identical** across all services — even a single extra space breaks signature verification
3. **Login again after adding new scopes** — scopes are embedded in the token at login time, not read from DB on each request
4. **Use `Authorization: Bearer <token>` header** — the filter does not check query parameters
5. **Token expiry is 15 minutes by default** — increase `JWT_EXPIRATION=86400000` in user-auth-service `.env` for development