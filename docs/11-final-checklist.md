# Section 11: Final Checklist

## 1. Title
Module 8 Microservices — Final Status Checklist and Verification Scripts

## 2. Internship module requirement
Provide a single summary table and a repeatable set of verification commands mentors/reviewers can run to confirm the state of every Module 8 subsection before internship portal submission.

## 3. Status summary table
| Section | Implementation Status | Evidence Required | GitHub/MR Link | Screenshot Attached | Ready for Internship Portal |
|---|---|---|---|---|---|
| Submodules Integration | Implemented (Kafka + gateway routing + 2 Feign-style clients) | Kafka UI screenshot, routed request | `<PASTE_LINK_HERE>` | [ ] | [ ] |
| Microservices Course | Documentation proof only | Course completion screenshot | `<PASTE_LINK_HERE>` | [ ] | [ ] |
| Service Discovery | **Proposed / Not yet implemented** | Eureka dashboard (once built) | `<PASTE_LINK_HERE>` | [ ] | [ ] |
| Load Balancing | **Proposed / Not yet implemented** | Multi-instance logs (once built) | `<PASTE_LINK_HERE>` | [ ] | [ ] |
| API Gateway | **Implemented** (Spring Cloud Gateway, ~50 routes, RBAC, rate limiting) | Route + rate-limit screenshot | `<PASTE_LINK_HERE>` | [ ] | [ ] |
| Resilience (Optional) | **Partial** (custom breaker in demand-service; Resilience4j dependency unused at gateway) | Circuit-open log | `<PASTE_LINK_HERE>` | [ ] | [ ] |
| Web Security | **Implemented** (JWT, BCrypt, RBAC, Redis blacklist) | 401/403/200 curl evidence | `<PASTE_LINK_HERE>` | [ ] | [ ] |
| Logs | **Partial** (correlation/trace ID real; JSON logging + ELK proposed) | Correlated log lines | `<PASTE_LINK_HERE>` | [ ] | [ ] |
| Metrics | **Partial** (Actuator everywhere; Prometheus gateway-only; Grafana proposed) | `/actuator/prometheus` output | `<PASTE_LINK_HERE>` | [ ] | [ ] |
| Traces | **Partial** (gateway trace ID propagation; OTLP export disabled by default; no backend wired) | Trace ID header screenshot | `<PASTE_LINK_HERE>` | [ ] | [ ] |

## 4. What was implemented in this project (rollup)
5 of 10 rows are genuinely implemented or partially implemented with real, working code (Submodules Integration, API Gateway, Web Security, Logs-partial, Metrics-partial, Traces-partial — six, if counting partials). Service Discovery and Load Balancing are honestly proposed only. Resilience is a mixed/partial case with one real custom implementation. This is a normal, defensible state for a capstone checkpoint — the goal of this checklist is accuracy, not appearing 100% complete.

## 5. Why this matters
Mentors reviewing capstone evidence need a fast way to see what's real vs. planned, without reading 3,000 lines of code. This table is that fast path — and every "Implemented" claim in it is backed by a specific file cited in the corresponding section doc.

## 6. Architecture explanation
See `00-module-8-overview.md` for the full system diagram.

## 7. Technologies used
See individual section files for the precise dependency list per concern.

## 8. Files inspected
All files listed in the "Files inspected" section of documents `01` through `10`.

## 9. Files changed or to be changed
Only new documentation files were created (this folder + root `README.md`). No application source code was modified.

## 10. Configuration details
See individual section files.

## 11. Docker Compose changes
None applied to the real `docker-compose.yml`. All proposed additions (Eureka, Prometheus, Grafana, Zipkin/Tempo, Loki) are shown as illustrative snippets only, in sections `03`, `04`, `08`, `09`, `10`.

## 12. Final run commands
```bash
docker compose up -d
docker compose ps
./mvnw clean install -DskipTests
# start each service in its own terminal, e.g.:
cd services/user-auth-service && ../../mvnw spring-boot:run
cd talentgrid-api-gateway-service && ../mvnw spring-boot:run
```

## 13. Full backend health check script
```bash
for port in 8080 8081 8082 8083 8084 8085 8086 8087 8088 8089 8091; do
  echo "port $port:"
  curl -s "http://localhost:$port/actuator/health"
  echo
done
```

## 14. Swagger check script
```bash
for port in 8081 8082 8083 8085 8086 8087 8088 8089 8091; do
  echo "port $port api-docs:"
  curl -s -o /dev/null -w "%{http_code}\n" "http://localhost:$port/v3/api-docs"
  echo "port $port swagger-ui:"
  curl -s -o /dev/null -w "%{http_code}\n" "http://localhost:$port/swagger-ui/index.html"
done
```
Note: several services set `springdoc.swagger-ui.enabled=false` / `springdoc.api-docs.enabled=false` by default (e.g. `interview-service`, `candidate-service`, `application-service`) — a `404` there is expected unless the flag is overridden via env var for local testing.

## 15. Docker Compose status check
```bash
docker compose ps
docker ps --format "table {{.Names}}\t{{.Ports}}\t{{.Status}}"
```

## 16. Final backend smoke test
```bash
curl -i http://localhost:8080/actuator/health
```
Then, with the gateway and `user-auth-service` running:
```bash
curl -i -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"<your-test-user>","password":"<your-test-password>"}'
curl -i http://localhost:8080/api/v1/demands \
  -H "Authorization: Bearer <TOKEN_FROM_LOGIN_RESPONSE>"
```

## 17. Troubleshooting
- **Ports already in use**: check with `lsof -nP -iTCP:<port> -sTCP:LISTEN` before starting a service; do not kill the frontend on `4200`.
- **Postgres port conflict**: set `POSTGRES_HOST_PORT` in root `.env` (see `docker-compose.yml` header comment) instead of touching a system Postgres install.
- **JWT errors across services**: `JWT_SECRET` must be identical in every service's environment.
- **Kafka consumer errors**: confirm `docker compose ps` shows the `broker` container healthy before starting any Spring Boot service that consumes events.
- **166 GB `secret-scan-report.txt` in repo root**: flagged during inspection for this documentation task — investigate/clean this up separately; it was not touched or deleted as part of this work.

## 18. Internship portal upload checklist
- [ ] All 12 files in `docs/module-8-microservices/` reviewed for accuracy
- [ ] Root `README.md` Module 8 section links to all 12 files and renders correctly on GitHub
- [ ] No real secrets (JWT secret, DB password, API keys) appear in any doc
- [ ] Screenshots attached where placeholders exist, or explicitly left as "not yet completed"
- [ ] `git diff` reviewed by you before committing
- [ ] Course completion proof (`02-microservices-course-proof.md`) only marked "Done" for genuinely completed modules
- [ ] MR/GitHub link placeholders replaced with the real link once a PR is opened

---
Back to: [00 Overview](00-module-8-overview.md) · Previous: [10 Traces](10-traces.md)
