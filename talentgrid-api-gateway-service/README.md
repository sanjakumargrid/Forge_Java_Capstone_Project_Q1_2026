# Talentgrid API Gateway Service

<!-- ![Java 17+](https://img.shields.io/badge/Java-17%2B-blue?logo=openjdk)
![Spring Boot 3.5.0](https://img.shields.io/badge/Spring%20Boot-3.5.0-6DB33F?logo=spring-boot)
![Redis](https://img.shields.io/badge/Redis-Backed-DC382D?logo=redis)
![License](https://img.shields.io/badge/License-Proprietary-lightgrey) -->


## Table of Contents

- [Architecture Overview](#architecture-overview)
- [Technology Stack](#technology-stack)
- [Project Structure](#project-structure)
- [Security & Identity](#security--identity)
- [Rate Limiting & Traffic Control](#rate-limiting--traffic-control)
- [Observability & Distributed Tracing](#observability--distributed-tracing)
- [Error Handling](#error-handling)
- [Route Map](#route-map)
- [RBAC Configuration](#rbac-configuration)
- [Getting Started](#getting-started)
- [Configuration Reference](#configuration-reference)
- [Docker Deployment](#docker-deployment)
- [Testing & Verification](#testing--verification)
- [Contributing](#contributing)

---

## Architecture Overview

The API Gateway sits between all client-facing consumers (web, mobile, third-party integrations) and the platform's backend microservices. It enforces a strict **zero-trust perimeter** — no request reaches a downstream service without first passing through authentication, authorization, rate limiting, and payload validation.

```
                          ┌─────────────────────────────────────────────┐
                          │         TALENTGRID API GATEWAY              │
                          │                 :8080                       │
   ┌──────────┐           │                                             │
   │  Web UI  │──────────▶│  ┌──────────────────────────────────────┐   │    ┌──────────────────┐
   └──────────┘           │  │  Filter Chain (ordered)              │   │    │ user-auth-service │
                          │  │                                      │   │    │       :8081       │
   ┌──────────┐           │  │  1. CorrelationIdFilter     (-100)   │   │───▶├──────────────────┤
   │ Mobile   │──────────▶│  │  2. TraceContextFilter      (-99)    │   │    │ demand-service    │
   └──────────┘           │  │  3. RequestSizeValidation   (-98)    │   │───▶│       :8082       │
                          │  │  4. RequestLoggingFilter    (-95)    │   │    ├──────────────────┤
   ┌──────────┐           │  │  5. SecurityHeadersFilter   (-10)    │   │───▶│ candidate-service │
   │Third Party│─────────▶│  │  6. JwtAuthenticationFilter (-2)     │   │    │       :8085       │
   └──────────┘           │  │  7. ScopeAuthorizationMgr   (-1)     │   │    ├──────────────────┤
                          │  │  8. RateLimiter (Redis)              │   │───▶│ application-service│
                          │  │  9. ResponseLoggingFilter   (MAX)    │   │    │       :8086       │
                          │  └──────────────────────────────────────┘   │    ├──────────────────┤
                          │                                             │───▶│ interview-service │
                          │              ┌────────────┐                 │    │       :8087       │
                          │              │   Redis     │                │    ├──────────────────┤
                          │              │  (blacklist │                │───▶│ workforce-service │
                          │              │  + rate lim)│                │    │       :8091       │
                          │              └────────────┘                 │    └──────────────────┘
                          └─────────────────────────────────────────────┘
```

### Design Principles

| Principle | Implementation |
|---|---|
| **Stateless** | No sessions, no sticky routing — all state lives in Redis or the JWT itself |
| **Non-blocking** | Built on Project Reactor / Netty; fully asynchronous I/O |
| **Zero business logic** | The gateway never reads or writes to any business database |
| **Defense in depth** | Multiple security layers: header stripping → JWT validation → token blacklist check → scope-based RBAC → rate limiting |
| **Fail-safe** | All errors return a consistent JSON error envelope with trace IDs for debugging |

---

## Technology Stack

| Category | Technology | Purpose |
|---|---|---|
| **Runtime** | Java 17+, Spring Boot 3.5.0 | Application foundation |
| **Gateway** | Spring Cloud Gateway (WebFlux / Reactor Netty) | Non-blocking reverse proxy and filter chain |
| **Authentication** | `io.jsonwebtoken:jjwt` | HMAC-SHA256 JWT signature verification |
| **Token Revocation** | Redis (reactive) | Real-time JWT blacklist lookups |
| **Rate Limiting** | `spring-cloud-gateway` + Redis | Token-bucket rate limiting per user/IP |
| **Observability** | OpenTelemetry, Micrometer, SLF4J/Logback | Distributed tracing, metrics, structured logging |
| **Build** | Maven 4.0+ | Dependency management and build lifecycle |
| **Container** | Docker (Eclipse Temurin 17 Alpine) | Production-ready container image |

---

## Project Structure

```
src/main/java/com/talentgrid/gateway/
├── GatewayApplication.java              # Spring Boot entry point
│
├── config/                              # Configuration & bean wiring
│   ├── RbacProperties.java              #   RBAC rules deserialized from rbac-rules.yml
│   ├── RedisConfig.java                 #   ReactiveRedisTemplate bean configuration
│   └── SecurityConfig.java              #   Spring Security WebFlux configuration
│
├── constants/                           # Application-wide constants
│   ├── AppConstants.java                #   Redis prefixes, payload size limits
│   ├── HeaderConstants.java             #   HTTP header names (X-User-Id, HSTS, CSP, etc.)
│   └── TraceConstants.java              #   Trace/span header constants
│
├── exception/                           # Centralized error handling
│   └── GlobalExceptionHandler.java      #   Unified JSON error responses with trace IDs
│
├── filter/                              # Gateway global filters (ordered)
│   ├── CorrelationIdFilter.java         #   Generates unique X-Correlation-Id per request
│   ├── RequestLoggingFilter.java        #   Logs inbound request method, path, and headers
│   ├── RequestSizeValidationFilter.java #   Rejects payloads exceeding 10 MB
│   ├── ResponseLoggingFilter.java       #   Logs outbound status code and latency
│   └── TraceContextFilter.java          #   Propagates/generates X-Trace-Id, populates MDC
│
├── ratelimit/                           # Rate limiting configuration
│   └── RedisRateLimitConfig.java        #   KeyResolver beans and RedisRateLimiter defaults
│
├── security/                            # Authentication & authorization
│   ├── JwtAuthenticationFilter.java     #   JWT parsing, signature verification, header injection
│   ├── JwtBlacklistService.java         #   Redis-backed token revocation checks
│   ├── ScopeAuthorizationManager.java   #   Path × method → scope enforcement
│   └── SecurityHeadersFilter.java       #   OWASP security response headers
│
└── util/                                # Shared utilities
    ├── JwtUtil.java                     #   JWT parsing helper methods
    └── TraceUtil.java                   #   OpenTelemetry trace ID extraction
```

```
src/main/resources/
├── application.properties               # Gateway routes, CORS, Redis, JWT, and actuator config
└── rbac-rules.yml                       # Externalized RBAC rules: public paths + scope mappings
```

---

## Security & Identity

The gateway implements a multi-layered security architecture. Every request passes through the following stages in order:

### 1. Security Headers (`SecurityHeadersFilter`)

All responses include OWASP-recommended headers to prevent common web vulnerabilities:

| Header | Value | Purpose |
|---|---|---|
| `X-Frame-Options` | `DENY` | Prevents clickjacking |
| `X-Content-Type-Options` | `nosniff` | Prevents MIME-type sniffing |
| `Strict-Transport-Security` | `max-age=31536000; includeSubDomains; preload` | Enforces HTTPS |
| `Content-Security-Policy` | `default-src 'self'` | Mitigates XSS and injection attacks |
| `X-XSS-Protection` | `1; mode=block` | Legacy XSS protection |

### 2. JWT Authentication (`JwtAuthenticationFilter`)

Every non-public request is authenticated via a stateless JWT flow:

1. Extracts the `Authorization: Bearer <token>` header
2. Verifies the HMAC-SHA256 signature against the configured secret
3. Validates expiration (`exp`), issuer (`iss`), and token type
4. Checks the token's JTI against the **Redis blacklist** to enforce logout/revocation
5. **Strips** any incoming identity headers (`X-User-Id`, `X-User-Roles`, `X-User-Scopes`, `X-User-Email`, `X-Auth-Time`) to prevent spoofing
6. **Injects** trusted identity headers extracted from the verified JWT for downstream services

```
Client Request                          Gateway                              Downstream Service
     │                                    │                                        │
     │  Authorization: Bearer <JWT>       │                                        │
     │───────────────────────────────────▶│                                        │
     │                                    │  1. Verify signature                   │
     │                                    │  2. Check expiration + issuer          │
     │                                    │  3. Check Redis blacklist              │
     │                                    │  4. Strip spoofed headers              │
     │                                    │  5. Inject trusted headers             │
     │                                    │──────────────────────────────────────▶ │
     │                                    │     X-User-Id: user123                 │
     │                                    │     X-User-Roles: ADMIN                │
     │                                    │     X-User-Scopes: DEMAND_VIEW,...     │
     │                                    │     X-User-Email: user@company.com     │
```

### 3. Scope-Based RBAC (`ScopeAuthorizationManager`)

After authentication, every request is matched against the externalized rules in `rbac-rules.yml`. The gateway checks:

- Whether the request **path + HTTP method** matches a rule
- Whether the user's JWT **scopes** include at least one of the `allowed-scopes`

If no rule matches, the request is **denied by default** (allowlist model). This ensures no unprotected endpoint is accidentally exposed.

### 4. Token Revocation (`JwtBlacklistService`)

When a user logs out via the Auth Service, the token's unique identifier (`jti`) is written to Redis with a TTL matching the token's remaining lifetime. The gateway checks this blacklist on every request, ensuring revoked tokens are rejected even before expiration.

---

## Rate Limiting & Traffic Control

Rate limiting uses Redis-backed **token bucket** algorithm via Spring Cloud Gateway's built-in `RequestRateLimiter` filter.

### Default Configuration

| Parameter | Value | Description |
|---|---|---|
| `replenishRate` | 30 requests/sec | Steady-state request rate per key |
| `burstCapacity` | 60 requests | Maximum burst size allowed |
| `requestedTokens` | 1 | Tokens consumed per request |

### Key Resolution Strategy

The default `userKeyResolver` applies intelligent key resolution:

1. **Authenticated users** → Rate limited by `X-User-Id` (per-user fairness)
2. **Unauthenticated requests** → Falls back to client IP address (per-IP protection)

Rate limit headers are exposed in every response: `X-RateLimit-Remaining`, `X-RateLimit-Limit`.

### Payload Size Validation

The `RequestSizeValidationFilter` rejects any request with a `Content-Length` exceeding **10 MB**, returning `413 Payload Too Large` before the request is forwarded downstream.

---

## Observability & Distributed Tracing

### Trace Propagation (`TraceContextFilter`)

Every request receives a unique trace identifier for end-to-end correlation across all microservices:

- If the client provides an `X-Trace-Id` header (e.g., from the frontend), it is propagated
- Otherwise, a new trace ID is generated using the active OpenTelemetry span
- The trace ID is injected into the SLF4J **MDC** context for structured log correlation

### Correlation IDs (`CorrelationIdFilter`)

A unique `X-Correlation-Id` (UUID v4) is generated for every inbound request. This ID is:
- Added to the outgoing request headers for downstream services
- Injected into the MDC context for log correlation
- Returned in the response headers for client-side debugging

### Request & Response Logging

| Filter | Logs |
|---|---|
| `RequestLoggingFilter` | HTTP method, request path, client IP, content length |
| `ResponseLoggingFilter` | HTTP status code, response latency (ms) |

All log entries include `traceId` and `correlationId` via MDC for cross-service traceability.

---

## Error Handling

The `GlobalExceptionHandler` intercepts all unhandled exceptions and returns a **consistent JSON error envelope**:

```json
{
  "timestamp": "2026-06-01T16:30:00.000Z",
  "status": 503,
  "error": "Service Unavailable",
  "message": "Downstream service is temporarily unavailable",
  "path": "/api/v1/demands",
  "traceId": "abc123def456"
}
```

| Scenario | HTTP Status | Message |
|---|---|---|
| Downstream unreachable (`Connection refused`) | `503` | Downstream service is temporarily unavailable |
| Invalid/expired/blacklisted JWT | `401` | Unauthorized |
| Insufficient RBAC scopes | `403` | Forbidden |
| Payload too large (>10 MB) | `413` | Payload Too Large |
| Rate limit exceeded | `429` | Too Many Requests |
| All other unhandled exceptions | `500` | An unexpected error occurred |

---

## Route Map

All routes are defined in `application.properties`. The gateway forwards requests to the appropriate backend service based on the URL path prefix.

| Route ID | Path Pattern | Target Service | Default Port | Auth Required |
|---|---|---|---|---|
| `user-auth-service` | `/api/v1/auth/**`, `/api/v1/admin/**` | Auth Service | `8081` | ❌ Public / ✅ Yes |
| `user-auth-service` | `/talentgrid/**` | Auth Service (rewritten) | `8081` | ❌ Public |
| `demand-service` | `/api/v1/demands/**`, `/api/v1/analytics/demands` | Demand Service | `8082` | ✅ Yes |
| `notification-service` | `/api/v1/notifications/**` | Notification Service | `8083` | ✅ Yes |
| `audit-service` | `/api/v1/audit/**` | Audit Service | `8084` | ✅ Yes |
| `candidate-service` | `/api/v1/candidates/**`, `/api/v1/gdpr/**`, `/api/v1/jobs/**` | Candidate Service | `8085` | ✅ Yes |
| `application-service`| `/api/v1/applications/**`, `/api/v1/analytics/pipeline` | Application Service| `8086` | ✅ Yes |
| `interview-service` | `/api/v1/interviews/**`, `/api/v1/recruiter/**` | Interview Service | `8087` | ✅ Yes |
| `offer-service` | `/api/v1/offers/**`, `/api/v1/webhooks/docusign` | Offer Service | `8088` | ✅ Yes / ❌ Public |
| `job-service` | `/api/v1/job-postings/**`, `/api/v1/careers/**` | Job Service | `8089` | ✅ Yes / ❌ Public |
| `ai-service` | `/api/v1/ai/**` (catch-all) | AI Service | `8090` | ✅ Yes |
| `workforce-service` | `/api/v1/engineers/**`, `/api/v1/utilisation/**` | Workforce Service | `8091` | ✅ Yes |
| _(internal)_ | `/actuator/health`, `/actuator/info` | Gateway self | `8080` | ❌ Public |

> **URL Rewriting:** All API endpoints support the `/talentgrid/` prefix in addition to `/api/v1/` (e.g. `/talentgrid/demands` or `/talentgrid/candidates`). The gateway uses a dynamic `RewritePath` filter to automatically translate these frontend-friendly URLs back to the internal `/api/v1/` structure expected by backend microservices. Specific aliases also exist for authentication paths (e.g. `/talentgrid/login` → `/api/v1/auth/login`).

---

## RBAC Configuration

RBAC rules are externalized in `rbac-rules.yml` and loaded at startup via `RbacProperties`. This allows security policy changes without code changes or redeployment (when backed by Spring Cloud Config Server).

### Structure

```yaml
rbac:
  public-paths:                          # Paths that skip JWT entirely
    - /api/v1/auth/**
    - /api/v1/careers/**
    - /actuator/health

  rules:                                 # Path × method → scope requirements
    - path: /api/v1/demands
      methods: [POST]
      allowed-scopes: [DEMAND_CREATE]

    - path: /api/v1/demands/*
      methods: [GET]
      allowed-scopes: [DEMAND_VIEW]
```

The complete RBAC configuration includes **85+ granular rules** covering all 5 team services, 85 unique permissions, and 6 roles. See [`API permissions`](./API%20permissions) for the full permission and role mapping documentation.

---

## Getting Started

### Prerequisites

| Requirement | Version | Purpose |
|---|---|---|
| Java (JDK) | 17+ | Runtime |
| Maven | 4.0+ | Build tool |
| Redis | 6.x+ | Rate limiting & token blacklist |
| Docker _(optional)_ | 20.x+ | Containerized Redis / deployment |

### Quick Start

```bash
# 1. Start Redis (if not already running)
docker run -d --name redis-talentgrid -p 6379:6379 redis:alpine

# 2. Build the project
mvn clean install

# 3. Run the API Gateway (starts on port 8080)
mvn spring-boot:run
```

### Verify

```bash
# Health check
curl http://localhost:8080/actuator/health

# Expected response:
# {"status":"UP","components":{"redis":{"status":"UP"},...}}
```

---

## Configuration Reference

All configuration is managed via `application.properties` with environment variable overrides for production deployment.

### Core Settings

| Property / Environment Variable | Description | Default |
|---|---|---|
| `server.port` | Gateway listen port | `8080` |
| `JWT_SECRET` | HMAC-SHA256 signing key (min 256-bit) | `changeme-replace-in-production-immediately` |
| `jwt.issuer` | Expected JWT issuer claim | `talentgrid-auth-service` |
| `CORS_ALLOWED_ORIGINS` | Global CORS allowed origins | `*` |
| `RATE_LIMIT_REPLENISH` | Steady-state request rate per user/IP | `30` |
| `RATE_LIMIT_BURST` | Maximum burst size allowed | `60` |
| `RATE_LIMIT_TOKENS` | Tokens consumed per request | `1` |

### Infrastructure

| Environment Variable | Description | Default |
|---|---|---|
| `REDIS_HOST` | Redis server hostname | `localhost` |
| `REDIS_PORT` | Redis server port | `6379` |

### Service Discovery

| Environment Variable | Description | Default Port |
|---|---|---|
| `USER_AUTH_SERVICE_HOST` | Auth service hostname | `localhost:8081` |
| `DEMAND_SERVICE_HOST` | Demand service hostname | `localhost:8082` |
| `NOTIFICATION_SERVICE_HOST` | Notification service hostname | `localhost:8083` |
| `AUDIT_SERVICE_HOST` | Audit service hostname | `localhost:8084` |
| `CANDIDATE_SERVICE_HOST` | Candidate service hostname | `localhost:8085` |
| `APPLICATION_SERVICE_HOST`| Application service hostname | `localhost:8086` |
| `INTERVIEW_SERVICE_HOST` | Interview service hostname | `localhost:8087` |
| `OFFER_SERVICE_HOST` | Offer service hostname | `localhost:8088` |
| `JOB_SERVICE_HOST` | Job service hostname | `localhost:8089` |
| `AI_SERVICE_HOST` | AI service hostname | `localhost:8090` |
| `WORKFORCE_SERVICE_HOST` | Workforce service hostname | `localhost:8091` |

### Actuator Endpoints

| Endpoint | Description |
|---|---|
| `/actuator/health` | Liveness & readiness probe (includes Redis health) |
| `/actuator/info` | Application info |
| `/actuator/metrics` | Micrometer metrics |
| `/actuator/prometheus` | Prometheus-compatible metrics scrape endpoint |

---

## Docker Deployment

### Build

```bash
# Build the JAR
mvn clean package -DskipTests

# Build the Docker image
docker build -t talentgrid-api-gateway-service:latest .
```

### Run

```bash
docker run -d \
  --name talentgrid-gateway \
  -p 8080:8080 \
  -e JWT_SECRET="<your-256-bit-production-secret>" \
  -e REDIS_HOST="redis" \
  -e USER_AUTH_SERVICE_HOST="user-auth-service" \
  -e DEMAND_SERVICE_HOST="demand-service" \
  -e NOTIFICATION_SERVICE_HOST="notification-service" \
  -e AUDIT_SERVICE_HOST="audit-service" \
  -e CANDIDATE_SERVICE_HOST="candidate-service" \
  -e APPLICATION_SERVICE_HOST="application-service" \
  -e INTERVIEW_SERVICE_HOST="interview-service" \
  -e OFFER_SERVICE_HOST="offer-service" \
  -e JOB_SERVICE_HOST="job-service" \
  -e AI_SERVICE_HOST="ai-service" \
  -e WORKFORCE_SERVICE_HOST="workforce-service" \
  talentgrid-api-gateway-service:latest
```

> ⚠️ **Production Note:** Always inject `JWT_SECRET` from a secrets manager (e.g., Kubernetes Secrets, AWS Secrets Manager, HashiCorp Vault). Never commit production secrets to version control.

---

## Testing & Verification

### JWT + Rate Limit Integration Test

This script generates a valid HMAC-SHA256 JWT and fires a burst of requests to verify both authentication and rate limiting are working correctly.

```bash
# 1. Generate a valid test token
TOKEN=$(python3 -c "
import hmac, hashlib, base64, json, time

def b64(d): return base64.urlsafe_b64encode(d).rstrip(b'=')

secret = b'changeme-replace-in-production-immediately'

header = {'alg': 'HS256', 'typ': 'JWT'}
payload = {
    'sub': 'user123',
    'email': 'test@talentgrid.com',
    'jti': 'mock-uuid-test-1234',
    'token_type': 'access',
    'roles': ['ADMIN'],
    'scopes': ['DEMAND_VIEW', 'DEMAND_CREATE', 'DEMAND_UPDATE', 'DEMAND_DELETE'],
    'iss': 'talentgrid-auth-service',
    'exp': int(time.time()) + 3600
}

msg = b64(json.dumps(header).encode()) + b'.' + b64(json.dumps(payload).encode())
sig = b64(hmac.new(secret, msg, hashlib.sha256).digest())
print((msg + b'.' + sig).decode())
")

echo "Generated Token: $TOKEN"

# 2. Test authenticated request
curl -s http://localhost:8080/api/v1/demands/ \
  -H "Authorization: Bearer $TOKEN" | jq .

# 3. Fire 150 rapid requests to verify rate limiting (burst=60, replenish=30/s)
echo "Firing 150 requests..."
for i in {1..150}; do
  curl -s -o /dev/null -w "%{http_code} " \
    "http://localhost:8080/api/v1/demands/" \
    -H "Authorization: Bearer $TOKEN"
done
echo ""
# Expected: First ~60 return 200, remaining return 429
```

---

## Contributing

1. Follow the existing package structure (`config/`, `filter/`, `security/`, `constants/`, `util/`)
2. All new filters must implement `GlobalFilter` + `Ordered` and document their order priority
3. Never add business logic, database access, or domain models to the gateway
4. RBAC changes go in `rbac-rules.yml` — update the `API permissions` document accordingly
5. All constants must be placed in the `constants/` package — no magic strings in filters or services

---

<p align="center">
  <strong>Talentgrid API Gateway Service</strong> · Part of the Talentgrid Workforce Intelligence Platform
</p>
