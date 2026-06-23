# TalentGrid API Gateway — Permissions Reference

Authoritative gateway enforcement rules live in [`src/main/resources/rbac-rules.yml`](src/main/resources/rbac-rules.yml).  
This document is a human-readable index of **public paths**, **scope-gated routes**, and **product role mappings**.

> **Path prefix:** All routed APIs are exposed as `/api/v1/...` (or `/talentgrid/...`, rewritten to `/api/v1/...`).  
> **Scope check:** The gateway requires the JWT to include **at least one** of the listed scopes (OR).  
> **Deny by default:** If a protected path has no matching rule → `403 GATEWAY_NO_RBAC_RULE`.  
> **Service layer:** Downstream services may apply additional checks (ownership, project PM, interviewer assignment).

---

## How enforcement works

```
Client → Gateway :8080
  1. JwtAuthenticationFilter   — validate JWT, inject X-User-Id / X-User-Scopes
  2. ScopeAuthorizationManager — match path + method against rbac-rules.yml
  3. Route proxy               — forward to microservice
```

| Result | HTTP | Code |
|--------|------|------|
| Public path | Pass (no JWT) | — |
| Missing / invalid JWT | 401 | `GATEWAY_UNAUTHORIZED` |
| No matching RBAC rule | 403 | `GATEWAY_NO_RBAC_RULE` |
| Scope not in token | 403 | `GATEWAY_FORBIDDEN` |

---

## Public paths (no JWT)

Configured in `rbac-rules.yml` → `public-paths`:

| Pattern | Purpose |
|---------|---------|
| `/api/v1/auth/**` | Login, register, refresh, logout |
| `/talentgrid/**` | Frontend-friendly auth aliases |
| `/api/v1/careers/**` | Public careers portal |
| `/api/v1/branding` | Public branding (GET) |
| `/api/v1/health/**` | Health probes |
| `/api/v1/jobs/public/**` | Public job listings |
| `/api/v1/webhooks/docusign` | DocuSign callback |
| `/api/v1/ai/careers/chat/**` | Public careers chatbot |
| `/actuator/health`, `/actuator/info` | Gateway health |

---

## Gateway-enforced endpoints

### Team 1 — Platform core & demand

| Method | Gateway path | Required scope(s) | Notes |
|--------|--------------|-------------------|-------|
| GET | `/api/v1/admin/users` | `USER_VIEW` | List/search users |
| POST | `/api/v1/admin/users` | `USER_CREATE` | Create user |
| GET | `/api/v1/admin/users/**` | `USER_VIEW` | User detail |
| PUT/PATCH | `/api/v1/admin/users/**` | `USER_UPDATE` | Update user |
| DELETE | `/api/v1/admin/users/**` | `USER_DELETE` | Delete user |
| PUT | `/api/v1/admin/users/**/roles` | `ROLE_ASSIGN` | Assign roles |
| GET | `/api/v1/admin/roles` | `ROLE_VIEW` | List roles |
| GET | `/api/v1/admin/roles/**/permissions` | `PERMISSION_VIEW` | Role permissions |
| PUT | `/api/v1/admin/roles/**/permissions` | `PERMISSION_ASSIGN` | Update role permissions |
| GET | `/api/v1/admin/permissions` | `PERMISSION_VIEW` | All permissions |
| GET | `/api/v1/users/**` | `USER_VIEW`, `DEMAND_VIEW`, or `DEMAND_STATUS_TRANSITION` | User lookup by id, RMG-by-location (proxied to user-auth) |
| GET | `/api/v1/projects/**` | `USER_VIEW`, `DEMAND_VIEW`, `DEMAND_CREATE`, `DEMAND_PM_APPROVE`, or `DEMAND_STATUS_TRANSITION` | Project by id, PM `mine-as-pm` list (proxied to user-auth) |
| GET | `/api/v1/accounts/**` | `USER_VIEW`, `DEMAND_VIEW`, `DEMAND_CREATE`, `DEMAND_PM_APPROVE`, or `DEMAND_STATUS_TRANSITION` | Account lookup by id (proxied to user-auth) |
| GET | `/api/v1/demands` | `DEMAND_VIEW` | Search demands (`?status=` supports multiple values) |
| GET | `/api/v1/demands/pm` | `DEMAND_VIEW` | PM-scoped demand list |
| POST | `/api/v1/demands` | `DEMAND_CREATE` | Create demand |
| GET | `/api/v1/demands/*` | `DEMAND_VIEW` | Demand detail |
| PATCH | `/api/v1/demands/*` | `DEMAND_UPDATE` | Update draft fields |
| DELETE | `/api/v1/demands/*` | `DEMAND_DELETE` | Soft-delete draft |
| POST | `/api/v1/demands/**/submit` | `DEMAND_SUBMIT` **or** `DEMAND_PM_APPROVE` | HM submit / PM auto-approve |
| POST | `/api/v1/demands/**/approve` | `DEMAND_PM_APPROVE` | PM approve/reject |
| PUT | `/api/v1/project-manager/**` | `DEMAND_PM_APPROVE` | PM approve route alias |
| POST | `/api/v1/demands/**/reject` | `DEMAND_PM_APPROVE` | PM reject |
| PATCH | `/api/v1/demands/**/status` | `DEMAND_STATUS_TRANSITION` | Workflow transition |
| GET | `/api/v1/demands/**/pipeline` | `DEMAND_PIPELINE_VIEW` | Hiring pipeline |
| GET | `/api/v1/demands/**/history` | `DEMAND_VIEW` | Status history |
| POST | `/api/v1/demands/**/nominations` | `DEMAND_NOMINATE` **or** `DEMAND_STATUS_TRANSITION` | RM nomination |
| POST | `/api/v1/demands/**/nominations/**/hm-decision` | `DEMAND_HM_NOMINATION_DECIDE` **or** `DEMAND_STATUS_TRANSITION` | HM decision |
| GET | `/api/v1/lookups/job-titles` | `DEMAND_VIEW` | Job title lookup |
| GET | `/api/v1/lookups/skills` | `DEMAND_VIEW` | Skill lookup |
| GET | `/api/v1/analytics/demands` | `ANALYTICS_DEMAND_VIEW` | Demand analytics |
| GET | `/api/v1/audit/**` | `AUDIT_VIEW` | Audit logs |
| GET | `/api/v1/notifications` | `NOTIFICATION_VIEW` | User notifications |
| GET | `/api/v1/ai/suggestions/skills` | `AI_SKILL_SUGGEST` | AI skill suggestions |

**Demand approval flow (service):** PM approval sets `APPROVED`; `SearchActivationScheduler` auto-activates `INTERNAL_SEARCH` (or `OPEN_EXTERNAL` for bench hiring).

---

### Team 2 — Talent acquisition

| Method | Gateway path | Required scope(s) |
|--------|--------------|-------------------|
| GET | `/api/v1/candidates` | `CANDIDATE_VIEW` |
| POST | `/api/v1/candidates` | `CANDIDATE_CREATE` |
| GET | `/api/v1/candidates/*` | `CANDIDATE_VIEW` |
| PUT | `/api/v1/candidates/*` | `CANDIDATE_UPDATE` |
| POST | `/api/v1/candidates/**/notes` | `CANDIDATE_NOTE_CREATE` |
| POST | `/api/v1/candidates/resume` | `RESUME_UPLOAD` |
| POST | `/api/v1/ai/candidates/**/score` | `AI_CANDIDATE_SCORE` |
| POST | `/api/v1/ai/candidates/**/rejection-email` | `AI_REJECTION_EMAIL_GENERATE` |
| POST | `/api/v1/ai/candidates/**/send-rejection-email` | `AI_REJECTION_EMAIL_SEND` |
| POST | `/api/v1/ai/interviews/**/questions` | `AI_INTERVIEW_QUESTIONS` |
| GET | `/api/v1/jobs/*` | `ASYNC_JOB_VIEW` |
| GET/POST | `/api/v1/applications` | `APPLICATION_VIEW` / `APPLICATION_CREATE` |
| POST | `/api/v1/applications/bulk` | `APPLICATION_BULK_ACTION` |
| GET | `/api/v1/applications/*` | `APPLICATION_VIEW` |
| PATCH | `/api/v1/applications/**/stage` | `APPLICATION_STAGE_MOVE` |
| GET/POST | `/api/v1/interviews` | `INTERVIEW_VIEW` / `INTERVIEW_SCHEDULE` |
| GET | `/api/v1/interviews/summary` | `INTERVIEW_VIEW` |
| GET | `/api/v1/recruiter/interviews/calendar` | `INTERVIEW_CALENDAR_VIEW` |
| GET/PATCH | `/api/v1/interviews/*` | `INTERVIEW_VIEW` / `INTERVIEW_UPDATE` |
| POST | `/api/v1/interviews/**/scorecards` | `SCORECARD_SUBMIT` |
| GET | `/api/v1/interviews/**/scorecards` | `SCORECARD_VIEW` |
| GET | `/api/v1/demands/**/scorecards` | `SCORECARD_VIEW` |
| POST | `/api/v1/offers` | `OFFER_CREATE` |
| GET | `/api/v1/offers/*` | `OFFER_VIEW` |
| PATCH | `/api/v1/offers/**/approve` | `OFFER_APPROVE` |
| PATCH | `/api/v1/offers/**/reject` | `OFFER_REJECT` |
| GET | `/api/v1/gdpr/candidates/**/export` | `GDPR_EXPORT` |
| POST | `/api/v1/gdpr/candidates/**/delete` | `GDPR_DELETE` |
| GET | `/api/v1/analytics/pipeline` | `ANALYTICS_PIPELINE_VIEW` |

> `SCORECARD_SUBMIT` is also enforced per-interview at the **service layer** (assigned interviewer), not only by role.

---

### Team 3 — Job posting & careers

| Method | Gateway path | Required scope(s) |
|--------|--------------|-------------------|
| GET/POST | `/api/v1/job-postings` | `JOB_POSTING_VIEW` / `JOB_POSTING_CREATE` |
| POST | `/api/v1/job-postings/from-demand/**` | `JOB_POSTING_CREATE` |
| GET/PUT/DELETE | `/api/v1/job-postings/*` | `JOB_POSTING_VIEW` / `JOB_POSTING_UPDATE` / `JOB_POSTING_DELETE` |
| POST | `/api/v1/job-postings/**/approve` | `JOB_POSTING_APPROVE` |
| POST | `/api/v1/job-postings/**/publish` | `JOB_POSTING_PUBLISH` |
| POST | `/api/v1/job-postings/**/unpublish` | `JOB_POSTING_UNPUBLISH` |
| GET | `/api/v1/job-postings/**/channels` | `JOB_POSTING_VIEW` |
| GET | `/api/v1/job-postings/**/analytics` | `ANALYTICS_POSTING_VIEW` |
| POST | `/api/v1/job-postings/**/referral-link` | `REFERRAL_CREATE` |
| PUT | `/api/v1/branding` | `BRANDING_UPDATE` |

Careers GET/apply endpoints are **public** (see public paths).

---

### Team 4 — AI service

| Method | Gateway path | Required scope(s) |
|--------|--------------|-------------------|
| POST | `/api/v1/ai/job-descriptions:generate` | `AI_JD_GENERATE` |
| POST | `/api/v1/ai/channels:recommend` | `AI_CHANNEL_RECOMMEND` |
| POST/GET | `/api/v1/ai/interactions` | `AI_INTERACTION_CREATE` / `AI_INTERACTION_VIEW` |

Careers chat paths under `/api/v1/ai/careers/chat/**` are **public**.

---

### Team 5 — Workforce allocation (core APIs)

| Method | Gateway path | Required scope(s) |
|--------|--------------|-------------------|
| GET | `/api/v1/engineer-profile/employees/*` | `WORKFORCE_PROFILE_VIEW` |
| PATCH | `/api/v1/engineer-profile/update` | `WORKFORCE_PROFILE_UPDATE` |
| GET | `/api/v1/engineer-profile/engineers` | `WORKFORCE_PROFILE_VIEW` |
| GET | `/api/v1/engineer-profile/employees-by-id/*` | `WORKFORCE_PROFILE_VIEW` |
| POST | `/api/v1/engineer-profile/hris-import` | `WORKFORCE_HRIS_IMPORT` |
| GET/POST | `/api/v1/engineers` | `ENGINEER_VIEW` / `ENGINEER_CREATE` |
| GET/PUT/DELETE | `/api/v1/engineers/*` | `ENGINEER_VIEW` / `ENGINEER_UPDATE` / `ENGINEER_DELETE` |
| PATCH | `/api/v1/engineers/me/self-service` | `ENGINEER_SELF_UPDATE` |
| GET | `/api/v1/engineers/bench-report` | `BENCH_VIEW` |
| POST | `/api/v1/engineers/search` | `ENGINEER_SEARCH` |
| GET/POST/PATCH/DELETE | `/api/v1/utilisation/**` | `UTILISATION_*` |
| GET | `/api/v1/utilisation/alerts` | `UTILISATION_ALERTS_VIEW` |
| POST | `/api/v1/rmg/nominations` | `WORKFORCE_NOMINATION_CREATE` |
| GET | `/api/v1/rmg/nominations/demand/*` | `WORKFORCE_NOMINATION_VIEW` |
| GET | `/api/v1/rmg/nominations/engineer/*` | `WORKFORCE_NOMINATION_VIEW` |
| GET/POST | `/api/v1/nominations` | `NOMINATION_VIEW` / `NOMINATION_CREATE` |
| GET | `/api/v1/nominations/*` | `NOMINATION_VIEW` |
| POST | `/api/v1/nominations/**/accept` | `NOMINATION_ACCEPT` |
| POST | `/api/v1/nominations/**/reject` | `NOMINATION_REJECT` |
| GET | `/api/v1/ai/match/**` | `AI_MATCH_VIEW` |
| POST | `/api/v1/ai/embed/engineer/**` | `AI_EMBEDDING_REFRESH` |
| GET/POST/PUT/DELETE | `/api/v1/demand-embeddings/**` | `DEMAND_EMBEDDING_*` |
| GET | `/api/v1/analytics/match-summary` | `ANALYTICS_MATCH_VIEW` |
| GET | `/api/v1/analytics/utilisation-summary` | `ANALYTICS_UTILISATION_VIEW` |

---

### Team 6 — RMG UI & upskilling

| Method | Gateway path | Required scope(s) |
|--------|--------------|-------------------|
| GET | `/api/v1/skill-gap/heatmap` | `WORKFORCE_SKILLGAP_VIEW` |
| GET | `/api/v1/skill-gap/summary` | `WORKFORCE_SKILLGAP_VIEW` |
| GET | `/api/v1/skill-gap/trends` | `WORKFORCE_SKILLGAP_VIEW` |
| POST | `/api/v1/skill-gap/refresh` | `WORKFORCE_SKILLGAP_REFRESH` |
| POST | `/api/v1/workforce/ai/upskilling/recommendations/generate` | `WORKFORCE_AI_UPSKILL_GENERATE` |
| GET | `/api/v1/workforce/ai/upskilling/recommendations/history` | `WORKFORCE_AI_UPSKILL_VIEW` |
| GET | `/api/v1/rmg-analytics-dashboard/**` | `WORKFORCE_ANALYTICS_VIEW` |
| GET | `/api/v1/rmg/demands` | `WORKFORCE_BENCH_SEARCH` **or** `WORKFORCE_NOMINATION_VIEW` |
| PATCH | `/api/v1/rmg/demands/**/cancel` | `DEMAND_STATUS_TRANSITION` |
| PATCH | `/api/v1/rmg/demands/**/status` | `DEMAND_STATUS_TRANSITION` |
| GET | `/api/v1/bench-filter/search` | `WORKFORCE_BENCH_SEARCH` |
| GET | `/api/v1/bench-filter/export/csv` | `WORKFORCE_REPORT_EXPORT` |
| GET | `/api/v1/rmg-search/search` | `WORKFORCE_BENCH_SEARCH` |
| GET | `/api/v1/bench-report` | `WORKFORCE_BENCH_SEARCH` |

---

## Scope catalog (gateway-enforced)

All scopes referenced in `rbac-rules.yml` (99 unique):

| Domain | Scopes |
|--------|--------|
| **Admin / RBAC** | `USER_VIEW`, `USER_CREATE`, `USER_UPDATE`, `USER_DELETE`, `ROLE_VIEW`, `ROLE_ASSIGN`, `PERMISSION_VIEW`, `PERMISSION_ASSIGN` |
| **Demand** | `DEMAND_VIEW`, `DEMAND_CREATE`, `DEMAND_UPDATE`, `DEMAND_DELETE`, `DEMAND_SUBMIT`, `DEMAND_PM_APPROVE`, `DEMAND_STATUS_TRANSITION`, `DEMAND_PIPELINE_VIEW`, `DEMAND_NOMINATE`, `DEMAND_HM_NOMINATION_DECIDE` |
| **Candidate / TA** | `CANDIDATE_VIEW`, `CANDIDATE_CREATE`, `CANDIDATE_UPDATE`, `CANDIDATE_NOTE_CREATE`, `RESUME_UPLOAD`, `APPLICATION_VIEW`, `APPLICATION_CREATE`, `APPLICATION_BULK_ACTION`, `APPLICATION_STAGE_MOVE` |
| **Interview / offer** | `INTERVIEW_VIEW`, `INTERVIEW_SCHEDULE`, `INTERVIEW_CALENDAR_VIEW`, `INTERVIEW_UPDATE`, `SCORECARD_SUBMIT`, `SCORECARD_VIEW`, `OFFER_CREATE`, `OFFER_VIEW`, `OFFER_APPROVE`, `OFFER_REJECT` |
| **Job posting** | `JOB_POSTING_VIEW`, `JOB_POSTING_CREATE`, `JOB_POSTING_UPDATE`, `JOB_POSTING_DELETE`, `JOB_POSTING_APPROVE`, `JOB_POSTING_PUBLISH`, `JOB_POSTING_UNPUBLISH`, `REFERRAL_CREATE`, `BRANDING_UPDATE` |
| **AI** | `AI_SKILL_SUGGEST`, `AI_CANDIDATE_SCORE`, `AI_REJECTION_EMAIL_GENERATE`, `AI_REJECTION_EMAIL_SEND`, `AI_INTERVIEW_QUESTIONS`, `AI_JD_GENERATE`, `AI_CHANNEL_RECOMMEND`, `AI_INTERACTION_CREATE`, `AI_INTERACTION_VIEW`, `AI_MATCH_VIEW`, `AI_EMBEDDING_REFRESH` |
| **Workforce** | `ENGINEER_VIEW`, `ENGINEER_CREATE`, `ENGINEER_UPDATE`, `ENGINEER_DELETE`, `ENGINEER_SELF_UPDATE`, `ENGINEER_SEARCH`, `BENCH_VIEW`, `WORKFORCE_PROFILE_VIEW`, `WORKFORCE_PROFILE_UPDATE`, `WORKFORCE_HRIS_IMPORT`, `WORKFORCE_NOMINATION_CREATE`, `WORKFORCE_NOMINATION_VIEW`, `WORKFORCE_SKILLGAP_VIEW`, `WORKFORCE_SKILLGAP_REFRESH`, `WORKFORCE_AI_UPSKILL_GENERATE`, `WORKFORCE_AI_UPSKILL_VIEW`, `WORKFORCE_ANALYTICS_VIEW`, `WORKFORCE_BENCH_SEARCH`, `WORKFORCE_REPORT_EXPORT` |
| **Utilisation / nominations** | `UTILISATION_VIEW`, `UTILISATION_CREATE`, `UTILISATION_UPDATE`, `UTILISATION_DELETE`, `UTILISATION_ALERTS_VIEW`, `NOMINATION_VIEW`, `NOMINATION_CREATE`, `NOMINATION_ACCEPT`, `NOMINATION_REJECT` |
| **Embeddings** | `DEMAND_EMBEDDING_VIEW`, `DEMAND_EMBEDDING_CREATE`, `DEMAND_EMBEDDING_UPDATE`, `DEMAND_EMBEDDING_DELETE` |
| **Analytics / governance** | `ANALYTICS_DEMAND_VIEW`, `ANALYTICS_PIPELINE_VIEW`, `ANALYTICS_POSTING_VIEW`, `ANALYTICS_MATCH_VIEW`, `ANALYTICS_UTILISATION_VIEW`, `AUDIT_VIEW`, `NOTIFICATION_VIEW`, `ASYNC_JOB_VIEW` |
| **GDPR** | `GDPR_EXPORT`, `GDPR_DELETE` |

**Legacy alias:** Product docs may still say `DEMAND_APPROVE` — the gateway enforces **`DEMAND_PM_APPROVE`**.

---

## Product role → scope mapping (reference)

Roles are assigned scopes in **user-auth-service**. The gateway only sees JWT scopes, not role names.

Legend: ✅ full | ⚡ scoped (own data/BU) | 🔍 read-only

### Key demand scopes by role

| Scope | ADMIN | PM | HM | RM/RMG | RECRUITER |
|-------|:-----:|:--:|:--:|:------:|:---------:|
| `DEMAND_CREATE` | ✅ | ✅ | ✅ | | |
| `DEMAND_VIEW` | ✅ | ⚡ | ⚡ | ✅ | ✅ |
| `DEMAND_UPDATE` | ✅ | ⚡ | ⚡ | | |
| `DEMAND_DELETE` | ✅ | ⚡ | ⚡ | | |
| `DEMAND_SUBMIT` | ✅ | ✅ | ✅ | | |
| `DEMAND_PM_APPROVE` | ✅ | ✅ | | | |
| `DEMAND_STATUS_TRANSITION` | ✅ | ⚡ | ⚡ | | |
| `DEMAND_PIPELINE_VIEW` | ✅ | ⚡ | ⚡ | ✅ | |
| `DEMAND_NOMINATE` | ✅ | | | ✅ | |
| `DEMAND_HM_NOMINATION_DECIDE` | ✅ | ✅ | ✅ | | |

### Workforce & RMG UI scopes

| Scope | ADMIN | RM/RMG | EMPLOYEE |
|-------|:-----:|:------:|:--------:|
| `WORKFORCE_BENCH_SEARCH` | ✅ | ✅ | |
| `WORKFORCE_SKILLGAP_VIEW` | ✅ | ✅ | |
| `WORKFORCE_AI_UPSKILL_VIEW` | ✅ | ✅ | 🔍 |
| `WORKFORCE_PROFILE_UPDATE` | | | ✅ |
| `ENGINEER_SELF_UPDATE` | | | ✅ |

For the full cross-role matrix used in product planning, see team API contracts in the repo `api contracts/` folder. Update gateway rules in `rbac-rules.yml` first, then sync this document.

---

## Maintenance checklist

When adding or changing an API:

1. Add route in `application.yml` (path rewrite to downstream service).
2. Add RBAC rule in `rbac-rules.yml` (path, methods, `allowed-scopes`).
3. Assign scope(s) to roles in **user-auth-service**.
4. Update this file and [`README.md`](README.md#rbac-configuration).

When in doubt, **`rbac-rules.yml` wins** over this document.
