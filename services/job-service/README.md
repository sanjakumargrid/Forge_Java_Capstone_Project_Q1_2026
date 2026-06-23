# TalentGrid — Team 3: Market Presence Core Portal 💼🌐

**Location:** Bangalore
**Team Size:** 6 (BE×5 · FE×1) — Mentor: Venkatesh Kokila
**Port:** `8082` (direct) · `8084` (via Nginx gateway)
**Context Path:** `/api` (flat — no `/api/t3` prefix)

---

## Monorepo layout

```
Forge_backend/backend/job-posting-service/
├── Dockerfile
├── pom.xml                          # forge-ai-guardrail dependency lives here too
└── src/main/java/com/talentgrid/jobposting/
    ├── controller/      # JobPostingController, DemandController, NotificationController, RecruiterAiController
    ├── service/         # JobPostingService, DemandService, PromptGuardService, SseEmitterService
    ├── integration/      # AiIntegrationClient — calls Team 4's ai-service
    ├── dto/              # request/ , response/ , integration/ , embedded/ (ChannelDto, AnalyticsDto)
    ├── entity/           # JobPosting, Demand, JobPostingApproval, Notification
    ├── enums/            # JobStatus, ApprovalAction
    ├── kafka/            # DemandEventConsumer, PortalEventProducer, PortalConfirmationConsumer
    ├── repository/       # Spring Data JPA
    ├── security/         # JwtTokenUtil, AuthenticatedUser
    ├── filter/           # JwtAuthFilter
    ├── config/           # SecurityConfig, RestClientConfig
    └── exception/        # GlobalExceptionHandler
```

**Build / test:**
```bash
cd Forge_backend/backend/job-posting-service && mvn clean package
```

**Run locally (outside Docker):**
```bash
mvn spring-boot:run
```

Full local stack (recommended) is run from the **repo root** via `docker-compose.yml` — see [Quick Start](#quick-start).

---

## What We Own

Status reflects what's actually implemented in code today, not just the design doc.

| Req | Feature | Status |
|-----|---------|--------|
| REQJP01 | Auto-create job posting from demand (`DEMAND_OPEN_EXTERNAL` via Kafka) | ✅ |
| REQJP02 | Rich text job posting editor (frontend) | ✅ |
| REQJP03 | Publish to LinkedIn Jobs / Indeed | ⚠️ **Stubbed** — `publishChannel()` flips the channel's status to `live` locally; no real LinkedIn/Indeed API call exists yet |
| REQJP04 | Auto-publish to careers portal after approval | ✅ — async via `portal-job-events` → portal confirms via `portal-confirmations` |
| REQJP05 | Job posting analytics API | ⚠️ **Data model only** — `AnalyticsDto` (`views`/`clicks`/`applyStarts`/`applyCompletions`) exists on `JobPosting` but nothing increments it yet |
| REQJP06 | Public careers portal (filters, responsive, WCAG) | Owned by frontend (`apps/careers`) — backend exposes `GET /api/job-postings/public/live` + SSE stream |
| REQJP07 | Apply Now flow + resume upload + confirmation email | Owned by Chennai Team 2 / careers app — not in this service |
| REQJP08 | Sync careers applicants to Chennai Team 2 | Not yet wired from this service |
| REQJP09 | SEO-friendly URLs, meta, JSON-LD | ❌ Not started |
| REQJP10 | Employer branding (Admin/HR editable) | ❌ Not started |
| REQJP11 | Referral links for authenticated engineers | ❌ Not started |
| — | AI JD generation (Team 4 `ai-service`) | ✅ |
| — | Recruiter/careers chatbot (Team 4 `ai-service`) | ✅ |
| — | Guardrail on AI **input** (prompt injection, bias, secrets, toxicity, PII) | ✅ |
| — | Guardrail on AI **output** (PII/secret/system-prompt leakage in generated text) | ✅ |

---

## Architecture

```
                         ┌──────────────────────────────────────────────┐
                         │        job-posting-service (Spring Boot)       │
                         │           port 8082 (8084 via gateway)         │
                         │                                                │
  CH-T1 (Kafka) ────────►│  @KafkaListener  demand-events  (DemandEventConsumer)
  Recruiter UI ──────────►│  POST /api/job-postings, /save-draft, /submit-for-approval
  Hiring Manager UI ─────►│  POST /api/job-postings/{id}/approve | /decline
  Recruiter UI ──────────►│  POST /api/job-postings/{id}/publish | /channels/{ch}/publish
  Recruiter UI ──────────►│  POST /api/job-postings/generate-jd     (Team 4 JD gen, guardrail-checked)
  Careers app ───────────►│  POST /api/recruiter-ai/chat            (Team 4 chatbot, guardrail-checked)
  Public ────────────────►│  GET  /api/job-postings/public/live
  Public (SSE) ──────────►│  GET  /api/job-postings/public/events
  Any (JWT) ─────────────►│  GET  /api/notifications/**
                         │                                                │
                         │  Kafka Producer ──► portal-job-events  ───────┼──► Career Portal
                         │  Kafka Consumer ◄── portal-confirmations ◄────┼─── Career Portal
                         │                                                │
                         │  External: Team 4 ai-service (172.18.155.x:8084)
                         │    via Mac-host socat relay → host.docker.internal:9999
                         │    (Docker Desktop can't reach LAN peers directly)
                         │  External: user-auth-service (JWT issuer, shared HMAC secret)
                         │  DB: PostgreSQL — job_posting_db                │
                         └──────────────────────────────────────────────┘
```

---

## Quick Start (Local)

### Prerequisites
- Java 17, Maven 3.9+, Docker Desktop
- `socat` if you need the Team 4 AI relay: `brew install socat`

### 1. Start the full stack (from repo root)
```bash
cd "Forge_FullStack 2"
cp .env.example .env   # fill in DB_USERNAME, DB_PASSWORD, JWT_SECRET, AI_SERVICE_BASE_URL
docker-compose up -d --build
```

### 2. Start the Team 4 AI relay (if `AI_SERVICE_BASE_URL` points at the relay)
```bash
./scripts/ai-relay.sh start      # localhost:9999 -> Team4's ai-service
./scripts/ai-relay.sh status     # check it's listening
```
Docker Desktop containers can't reach other devices on your LAN/Wi-Fi directly — only the host and the internet — so `job-posting-service` reaches Team 4 via `host.docker.internal:9999`, not the LAN IP directly. See the repo-root `README.md` for the full diagnostic chain if AI calls fail.

### 3. Verify
```bash
curl http://localhost:8082/actuator/health
curl http://localhost:8084/api/job-postings/public/live   # via gateway
```

---

## API Reference

Full table in [`../api_ri.md`](../api_ri.md). Summary:

| Group | Base path | Auth |
|---|---|---|
| Job Postings | `/api/job-postings/**` | JWT (except `/public/**`) |
| Demands (from Kafka) | `/api/demands/**` | JWT |
| Notifications | `/api/notifications/**` | JWT |
| Recruiter AI | `/api/recruiter-ai/chat` | Public |

### Generate JD (guardrail-checked, calls Team 4)
```bash
curl -X POST http://localhost:8084/api/job-postings/generate-jd \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "demandId": "101",
    "roleTitle": "Senior Java Developer",
    "department": "Engineering",
    "location": "Bangalore",
    "workMode": "HYBRID",
    "experienceYears": "5",
    "skillsRequired": ["Java", "Spring Boot", "Kafka"],
    "seniorityLevel": "SENIOR",
    "employmentType": "FULL_TIME",
    "additionalContext": "Looking for someone with strong distributed systems experience."
  }'
```
`additionalContext` and the AI-generated sections are both run through `PromptGuardService` before reaching Team 4 / before reaching the caller. A `400` with `"Prompt was blocked by security guardrail"` means the guardrail fired — see [Guardrail integration](#guardrail-integration-forge-ai-guardrail) below.

### Careers chatbot (public, guardrail-checked)
```bash
curl -X POST http://localhost:8084/api/recruiter-ai/chat \
  -H "Content-Type: application/json" \
  -d '{"sessionId":"sess-1","message":"What does this role involve?","teamId":"BACKEND","featureType":"CAREERS_CHATBOT"}'
```

---

## Kafka Topics

| Topic | Direction | Counterparty | Purpose |
|---|---|---|---|
| `demand-events` | consume | CH-T1 (demand service) | `DEMAND_OPEN_EXTERNAL` → auto-create draft job posting |
| `portal-job-events` | produce | Career Portal | `JOB_PUBLISHED` / `JOB_UNPUBLISHED` |
| `portal-confirmations` | consume | Career Portal | `JOB_LIVE` / `JOB_TAKEN_DOWN` / `JOB_FAILED` — drives channel status + recruiter notification |

Full event schemas: [`../CAREER_PORTAL_INTEGRATION.md`](../CAREER_PORTAL_INTEGRATION.md).

---

## Database Schema

| Table | Purpose |
|---|---|
| `demands` | Demand records consumed from Kafka |
| `demand_skills` | Skills collection for demands |
| `job_postings` | Core posting records — includes `channels` (JSON, per-channel status) and `analytics` (JSON, currently unused placeholder) |
| `job_posting_skills` | Skills collection for postings |
| `job_posting_approvals` | Audit trail of every approve/decline/publish action |
| `notifications` | In-app notifications per user |

---

## Guardrail integration (`forge-ai-guardrail`)

Same private library Team 4 uses (`com.gridynamics.forge:forge-ai-guardrail:2.0.1`, JFrog), wired via `PromptGuardService`:

- **Input side** — `additionalContext` (JD generation) and `message` (chatbot) are checked via `GuardrailEngine.evaluate()` before being sent to Team 4. Blocks on prompt injection, discriminatory hiring language (gender/religion/caste/nationality/race/disability/age/sexuality), toxicity, and embedded secrets (AWS keys, API keys, private keys, JWTs). PII is sanitized (redacted), not blocked.
- **Output side** — Team 4's generated JD sections and chatbot replies are checked via `GuardrailEngine.validateOutput()` before reaching the caller (PII/secret/system-prompt leakage).
- **Config** (`forge.guardrail.*` in `application.yml`): `production-mode: true`, `semantic.enabled: false` (would need Redis + an ONNX model — infra is present but not wired), `api.enabled: false` (we call the engine in-process, not over HTTP).
- A `400` response with `"Prompt was blocked by security guardrail"` is the guardrail firing — distinct from a `502` (`"AI service unavailable"`), which means Team 4 itself is unreachable/down.

---

## Dependencies

### We NEED from other teams
| Team | What we need |
|---|---|
| CH-T1 | `demand-events` Kafka topic, JWT-issuing auth service |
| BL-T4 | `ai-service` JD generation + chatbot endpoints (`/api/t4/v1/ai/jd/generate`, `/api/t4/v1/chatbot/chat`) |
| CH-T2 | Candidate/application intake API (not yet integrated) |

### We PROVIDE to other teams
| Team | What we provide |
|---|---|
| Career Portal | `portal-job-events` Kafka topic, public live-jobs REST + SSE endpoints |
| BL-T4 | Job posting publish events (for their analytics — not yet wired) |
| CH-T2 | Posting/demand correlation data (for application routing — not yet wired) |

---

## Known Gaps

- **LinkedIn/Indeed publishing is simulated.** `JobPostingService.publishChannel()` sets the channel status to `live` directly — there's no OAuth2/XML feed call to either platform.
- **Analytics fields are unpopulated.** `views`/`clicks`/`applyStarts`/`applyCompletions` exist on every posting but nothing increments them — no tracking endpoint or pixel exists yet.
- **No SEO, referral links, or employer branding** backend support yet (REQJP09–11).
- Full endpoint-level gap analysis (backend exposes vs. frontend calls): [`../api_ri.md`](../api_ri.md#gap-analysis).

---

## Key Design Decisions

1. **Kafka-driven demand intake** — decouples job-posting creation from the demand service; duplicate `demandId` events are silently skipped, no dual-write needed (see `CHANGES.md`).
2. **Async portal publish via confirmation pattern** — `portal-job-events` out / `portal-confirmations` in, with `pending` → `live`/`failed` channel states, rather than a synchronous call to the portal.
3. **Guardrail on both sides of the AI call** — not just the user's prompt, but Team 4's generated output too, since an LLM can echo back injected content or leak PII it was fed.
4. **Host-side relay for the AI dependency** — Docker Desktop on Mac can't route to arbitrary LAN peers, so the AI service base URL points at a `socat` relay on the host (`host.docker.internal:9999`) rather than the LAN IP directly. Documented in the repo-root `README.md`.
5. **JWT shared secret, not a token-introspection call** — `job-posting-service` validates JWTs itself (`JwtTokenUtil`) using the same HMAC secret as `user-auth-service`, avoiding a network round-trip per request.
