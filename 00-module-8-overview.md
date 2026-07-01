# Module 8: Microservices — Overview

## 1. Title
Module 8 — Microservices: Overview and Status Summary (TalentGrid / Forge Java Capstone)

## 2. Internship module requirement
Grid Dynamics Java internship capstone, Module 8, requires demonstrating microservices architecture concepts in a real project: service decomposition, service discovery, load balancing, an API gateway, resilience patterns, security, and observability (logs, metrics, traces).

## 3. What was implemented in this project
TalentGrid is a real multi-service Spring Boot system, built by multiple teams, with:
- 9 domain/business microservices + 1 API Gateway + 2 shared infrastructure services (audit, notification), all under one Maven reactor build.
- A working Spring Cloud Gateway as the single entry point for the Angular frontend.
- JWT-based security enforced at the gateway and re-checked in every downstream service.
- Kafka-based asynchronous integration between services (event producers/consumers).
- Redis used for JWT blacklisting and gateway rate limiting.
- PostgreSQL (pgvector image) as the shared relational database, with per-service Flyway-style migrations.
- Partial observability: correlation IDs and trace IDs propagated at the gateway, Actuator health endpoints everywhere, Prometheus metrics on the gateway only.

## 4. What is NOT implemented (honest status)
- **Service Discovery (Eureka)** — not implemented. Services are wired by static host:port environment variables.
- **Client-side Load Balancing** — not implemented. There is exactly one instance per service; no `@LoadBalanced` / Spring Cloud LoadBalancer / DiscoveryClient usage found.
- **Resilience4j** — dependency added to the gateway only, not actually wired with annotations or config. `demand-service` has its own custom circuit breaker for AI calls (not Resilience4j).
- **Centralized/structured logging (ELK / ECK)** — not implemented. `logstash-logback-encoder` is a dependency but no `logback-spring.xml` activates JSON logging yet.
- **Prometheus + Grafana dashboards** — Prometheus scraping endpoint only exists on the gateway; Grafana folder is an empty placeholder.
- **Distributed tracing backend (Zipkin/Tempo)** — trace IDs are generated and propagated at the gateway, but OTLP export is disabled by default and no tracing backend is wired into this repo's `docker-compose.yml`.

These gaps are documented per-section below as **Proposed / Not yet implemented**, with a concrete plan to close them — this is intentional, per capstone guidance, rather than a claim of false completion.

## 5. Why this matters for microservices
A microservices architecture is only as good as its cross-cutting concerns: if services can't discover each other, can't survive a partner service being down, and can't be observed in production, then splitting a monolith into services adds operational cost without the reliability benefit. Module 8 exists to prove the team understands (and where possible, has implemented) these concerns.

## 6. Architecture explanation
```
Frontend (Angular, :4200)
        │
        ▼
API Gateway (Spring Cloud Gateway, :8080)
 - JWT validation, RBAC, rate limiting (Redis), CORS
        │
        ├──► user-auth-service (:8081)
        ├──► demand-service (:8082)
        ├──► notification-service (:8083)
        ├──► audit-service (:8084)
        ├──► candidate-service (:8085)
        ├──► application-service (:8086)
        ├──► interview-service (:8087)
        ├──► offer-service (:8088)
        ├──► job-service (:8089)
        ├──► AI service (external, :8090, not in this repo)
        └──► workforce-service (:8091)

Shared infrastructure: PostgreSQL, Redis, Kafka (KRaft), Kafka UI
```
See `01-submodules-integration.md` for a detailed Mermaid diagram and a service dependency table.

## 7. Technologies used
Java 17+, Spring Boot 3.x, Spring Cloud Gateway (WebFlux), Spring Security, Spring Data JPA, Spring Kafka, Spring Data Redis, PostgreSQL (pgvector), Kafka (KRaft mode, no ZooKeeper), Redis, Maven multi-module reactor, JJWT (JWT), Micrometer, OpenTelemetry bridge (gateway only), springdoc-openapi (Swagger).

## 8. Files inspected
- Root `pom.xml` (module list, dependency management)
- `docker-compose.yml`
- `structure.txt`
- `.env.example`
- Every `services/*/pom.xml`, `application.properties` / `application.yml`
- `talentgrid-api-gateway-service/src/main/resources/application.yml` and `rbac-rules.yml`
- `talentgrid-api-gateway-service/src/main/java/.../filter/CorrelationIdFilter.java`, `TraceContextFilter.java`
- `talentgrid-kafka/src/main/java/.../consumer/BaseKafkaConsumer.java`
- `talentgrid-clients/audit-client`, `talentgrid-clients/notification-client`, `talentgrid-clients/auth-client`
- `observability/`, `talentgrid-observability/` (both mostly empty placeholder folders)
- `services/demand-service/src/main/java/.../ai/SimpleCircuitBreaker.java`
- `services/user-auth-service/src/main/java/.../controller/AuthController.java`

## 9. Files changed or files to be changed
No source code was changed while writing this documentation. New files created:
- `docs/module-8-microservices/*.md` (this set of 12 files)
- `README.md` at repo root (did not previously exist)

## 10. Configuration details
See individual section files (`03`–`10`) for exact configuration keys and values (secrets shown as placeholders only).

## 11. Docker Compose changes
None made. Proposed Docker Compose additions (Eureka, Prometheus, Grafana, Zipkin/Tempo, ELK/Loki) are shown as snippets in the relevant section files, not applied to the real `docker-compose.yml`.

## 12. Local run commands
```bash
docker compose up -d
./mvnw clean install -DskipTests
# then run each service module, e.g.:
cd services/user-auth-service && ../../mvnw spring-boot:run
```

## 13. Verification commands
```bash
curl -i http://localhost:8080/actuator/health
```

## 14. Expected output
`200 OK` with a JSON body such as `{"status":"UP", ...}` once the gateway and its Redis dependency are healthy.

## 15. Screenshots to attach
- [ ] Screenshot of `docker compose ps` showing Kafka/Postgres/Redis running
- [ ] Screenshot of all services starting successfully in IDE/terminal
- [ ] Screenshot of gateway health check response

`Screenshot: <ATTACH_SCREENSHOT_HERE>`

## 16. GitHub/Merge Request link
`GitHub/MR Link: <PASTE_LINK_HERE>`

## 17. Troubleshooting
- If a service fails to start, check that `JWT_SECRET` is identical across all services (many services validate the same shared secret).
- If Postgres port conflicts with a local install, set `POSTGRES_HOST_PORT` in `.env` (see `docker-compose.yml` header comments).
- If Kafka consumers throw connection errors, confirm `docker compose ps` shows the `broker` container healthy before starting Spring Boot services.

## 18. Mentor review checklist
- [ ] Confirms which sections are implemented vs. proposed, with evidence
- [ ] Confirms no secrets are exposed in any doc file
- [ ] Confirms local run instructions actually work
- [ ] Confirms screenshots attached before portal submission

---
See also: [01 Submodules Integration](01-submodules-integration.md) · [11 Final Checklist](11-final-checklist.md)
