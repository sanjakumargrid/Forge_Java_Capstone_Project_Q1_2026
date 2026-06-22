# TALENTGRID — API Endpoints, Permissions & Role Mapping


---

## 1. All API Endpoints → Permissions

### Team 1 — Platform Core & Demand Intelligence

| Method | Endpoint | Permission | Notes |
|--------|----------|------------|-------|
| POST | `/auth/login` | `PUBLIC` | No auth required (Google SSO exchange) |
| POST | `/auth/refresh` | `PUBLIC` | No auth required (cookie-based) |
| GET | `/auth/me` | `AUTH_SELF` | Any authenticated user |
| POST | `/auth/logout` | `AUTH_SELF` | Any authenticated user |
| GET | `/admin/users` | `USER_VIEW` | Search enterprise users |
| PUT | `/admin/users/{id}/roles` | `ROLE_ASSIGN` | Assign roles to user |
| GET | `/admin/roles` | `ROLE_VIEW` | List all enterprise roles |
| GET | `/admin/roles/{role}/permissions` | `PERMISSION_VIEW` | Get permissions for a role |
| PUT | `/admin/roles/{role}/permissions` | `PERMISSION_ASSIGN` | Replace permissions on a role |
| GET | `/admin/permissions` | `PERMISSION_VIEW` | List all available permissions |
| GET | `/demands` | `DEMAND_VIEW` | Search/list demands |
| POST | `/demands` | `DEMAND_CREATE` | Create workforce demand |
| GET | `/demands/{id}` | `DEMAND_VIEW` | Get demand detail |
| PATCH | `/demands/{id}` | `DEMAND_UPDATE` | Update editable demand fields |
| DELETE | `/demands/{id}` | `DEMAND_DELETE` | Soft delete draft demand |
| POST | `/demands/{id}/submit` | `DEMAND_SUBMIT` or `DEMAND_PM_APPROVE` | Submit from draft (HM vs PM auto-approve) |
| POST | `/demands/{id}/approve` | `DEMAND_PM_APPROVE` | PM for the demand's project approves or rejects (`CLOSED` + `PM_REJECTED`) |
| PUT | `/project-manager/demands/{demandId}/approve` | `DEMAND_PM_APPROVE` | PM approves pending demand for own project |
| PATCH | `/demands/{id}/status` | `DEMAND_STATUS_TRANSITION` | Perform legal workflow transition |
| GET | `/demands/{id}/pipeline` | `DEMAND_PIPELINE_VIEW` | Unified hiring pipeline view |
| GET | `/demands/{id}/history` | `DEMAND_VIEW` | Status history audit trail |
| POST | `/demands/{id}/nominations` | `DEMAND_NOMINATE` or `DEMAND_STATUS_TRANSITION` | RM internal nomination |
| POST | `/demands/{id}/nominations/{nid}/hm-decision` | `DEMAND_HM_NOMINATION_DECIDE` or `DEMAND_STATUS_TRANSITION` | HM accept/reject nomination |
| GET | `/analytics/demands` | `ANALYTICS_DEMAND_VIEW` | Demand analytics dashboard |
| GET | `/audit/logs` | `AUDIT_VIEW` | Search audit logs |
| GET | `/notifications` | `NOTIFICATION_VIEW` | Get user notifications |
| GET | `/ai/suggestions/skills` | `AI_SKILL_SUGGEST` | AI-generated skill suggestions |

---

### Team 2 — Talent Acquisition Engine

| Method | Endpoint | Permission | Notes |
|--------|----------|------------|-------|
| GET | `/health` | `PUBLIC` | Liveness probe, no auth |
| GET | `/health/ready` | `PUBLIC` | Readiness probe, no auth |
| GET | `/candidates` | `CANDIDATE_VIEW` | List/filter candidates |
| POST | `/candidates` | `CANDIDATE_CREATE` | Create candidate profile |
| GET | `/candidates/{candidateId}` | `CANDIDATE_VIEW` | Get full candidate profile |
| PUT | `/candidates/{candidateId}` | `CANDIDATE_UPDATE` | Update candidate profile |
| POST | `/candidates/{candidateId}/notes` | `CANDIDATE_NOTE_CREATE` | Add note/tags to candidate |
| POST | `/candidates/resume` | `RESUME_UPLOAD` | Upload resume (async parse) |
| POST | `/ai/candidates/{candidateId}/score` | `AI_CANDIDATE_SCORE` | AI score candidate vs demand |
| POST | `/ai/candidates/{candidateId}/rejection-email` | `AI_REJECTION_EMAIL_GENERATE` | Generate rejection email draft |
| POST | `/ai/candidates/{candidateId}/send-rejection-email` | `AI_REJECTION_EMAIL_SEND` | Dispatch rejection email |
| POST | `/ai/interviews/{interviewId}/questions` | `AI_INTERVIEW_QUESTIONS` | Generate interview questions |
| GET | `/jobs/{jobId}` | `ASYNC_JOB_VIEW` | Poll async job status |
| GET | `/applications` | `APPLICATION_VIEW` | List applications for demand |
| POST | `/applications` | `APPLICATION_CREATE` | Link candidate to demand |
| POST | `/applications/bulk` | `APPLICATION_BULK_ACTION` | Bulk pipeline action |
| GET | `/applications/{applicationId}` | `APPLICATION_VIEW` | Get full application detail |
| PATCH | `/applications/{applicationId}/stage` | `APPLICATION_STAGE_MOVE` | Move pipeline stage |
| GET | `/interviews` | `INTERVIEW_VIEW` | List interviews with filters |
| POST | `/interviews` | `INTERVIEW_SCHEDULE` | Schedule interview (Google Cal) |
| GET | `/interviews/summary` | `INTERVIEW_VIEW` | Interview dashboard summary |
| GET | `/recruiter/interviews/calendar` | `INTERVIEW_CALENDAR_VIEW` | Recruiter calendar view |
| GET | `/interviews/{interviewId}` | `INTERVIEW_VIEW` | Get interview details |
| PATCH | `/interviews/{interviewId}` | `INTERVIEW_UPDATE` | Cancel or reschedule interview |
| POST | `/interviews/{interviewId}/scorecards` | `SCORECARD_SUBMIT` | Submit scorecard |
| GET | `/interviews/{interviewId}/scorecards` | `SCORECARD_VIEW` | List scorecards for interview |
| GET | `/demands/{demandId}/scorecards` | `SCORECARD_VIEW` | All scorecards under demand |
| POST | `/offers` | `OFFER_CREATE` | Generate offer + approval chain |
| GET | `/offers/{offerId}` | `OFFER_VIEW` | Get offer with approval status |
| PATCH | `/offers/{offerId}/approve` | `OFFER_APPROVE` | Record approval in chain |
| PATCH | `/offers/{offerId}/reject` | `OFFER_REJECT` | Reject offer |
| POST | `/webhooks/docusign` | `PUBLIC` | DocuSign callback (HMAC-verified) |
| GET | `/analytics/pipeline` | `ANALYTICS_PIPELINE_VIEW` | Pipeline conversion metrics |
| GET | `/gdpr/candidates/{candidateId}/export` | `GDPR_EXPORT` | DSAR — export candidate PII |
| POST | `/gdpr/candidates/{candidateId}/delete` | `GDPR_DELETE` | Hard-delete candidate PII |

---

### Team 3 — Job Posting & Careers Portal

| Method | Endpoint | Permission | Notes |
|--------|----------|------------|-------|
| GET | `/job-postings` | `JOB_POSTING_VIEW` | List job postings (internal) |
| POST | `/job-postings` | `JOB_POSTING_CREATE` | Create job posting |
| POST | `/job-postings/from-demand/{demandId}` | `JOB_POSTING_CREATE` | Pre-fill posting from demand |
| GET | `/job-postings/{postingId}` | `JOB_POSTING_VIEW` | Get a job posting |
| PUT | `/job-postings/{postingId}` | `JOB_POSTING_UPDATE` | Update job posting |
| DELETE | `/job-postings/{postingId}` | `JOB_POSTING_DELETE` | Close/delete posting |
| POST | `/job-postings/{postingId}/approve` | `JOB_POSTING_APPROVE` | Approve posting (auto-publishes) |
| POST | `/job-postings/{postingId}/publish` | `JOB_POSTING_PUBLISH` | One-click publish to channels |
| POST | `/job-postings/{postingId}/unpublish` | `JOB_POSTING_UNPUBLISH` | Unpublish from channels |
| GET | `/job-postings/{postingId}/channels` | `JOB_POSTING_VIEW` | Get channel publish status |
| GET | `/job-postings/{postingId}/analytics` | `ANALYTICS_POSTING_VIEW` | Per-posting analytics |
| GET | `/careers` | `PUBLIC` | Public job listing (no auth) |
| GET | `/careers/{slug}` | `PUBLIC` | Public posting detail (no auth) |
| POST | `/careers/{slug}/apply` | `PUBLIC` | Apply Now (no auth) |
| GET | `/branding` | `PUBLIC` | Public branding content (no auth) |
| PUT | `/branding` | `BRANDING_UPDATE` | Update employer branding |
| POST | `/job-postings/{postingId}/referral-link` | `REFERRAL_CREATE` | Generate referral link |

---

### Team 4 — AI Service

| Method | Endpoint | Permission | Notes |
|--------|----------|------------|-------|
| GET | `/health` | `PUBLIC` | Liveness probe, no auth |
| POST | `/ai/job-descriptions:generate` | `AI_JD_GENERATE` | Generate structured JD |
| POST | `/ai/careers/chat` | `AI_CAREERS_CHAT` | RAG FAQ chat (JSON) |
| POST | `/ai/careers/chat:stream` | `AI_CAREERS_CHAT` | RAG FAQ chat (SSE stream) |
| POST | `/ai/channels:recommend` | `AI_CHANNEL_RECOMMEND` | Rank publish channels |
| POST | `/ai/interactions` | `AI_INTERACTION_CREATE` | Record LLM interaction |
| GET | `/ai/interactions` | `AI_INTERACTION_VIEW` | Query AI interactions |

---

### Team 5 — Internal Workforce Allocation

| Method | Endpoint | Permission | Notes |
|--------|----------|------------|-------|
| GET | `/engineers` | `ENGINEER_VIEW` | List all engineers |
| POST | `/engineers` | `ENGINEER_CREATE` | Create engineer profile |
| GET | `/engineers/{id}` | `ENGINEER_VIEW` | Get engineer by ID |
| PUT | `/engineers/{id}` | `ENGINEER_UPDATE` | Full update engineer profile |
| DELETE | `/engineers/{id}` | `ENGINEER_DELETE` | Soft-delete engineer |
| PATCH | `/engineers/me/self-service` | `ENGINEER_SELF_UPDATE` | Self-service skills/availability |
| GET | `/engineers/bench-report` | `BENCH_VIEW` | Bench report by availability |
| POST | `/engineers/search` | `ENGINEER_SEARCH` | Multi-criteria search |
| GET | `/utilisation` | `UTILISATION_VIEW` | List utilisation records |
| POST | `/utilisation` | `UTILISATION_CREATE` | Create utilisation record |
| GET | `/utilisation/{id}` | `UTILISATION_VIEW` | Get utilisation record |
| PATCH | `/utilisation/{id}` | `UTILISATION_UPDATE` | Update utilisation record |
| DELETE | `/utilisation/{id}` | `UTILISATION_DELETE` | Soft-delete utilisation record |
| GET | `/utilisation/alerts` | `UTILISATION_ALERTS_VIEW` | Over/under allocation alerts |
| GET | `/nominations` | `NOMINATION_VIEW` | List nominations |
| POST | `/nominations` | `NOMINATION_CREATE` | Nominate engineer for demand |
| GET | `/nominations/{id}` | `NOMINATION_VIEW` | Get nomination detail |
| POST | `/nominations/{id}/accept` | `NOMINATION_ACCEPT` | HM accepts nomination |
| POST | `/nominations/{id}/reject` | `NOMINATION_REJECT` | HM rejects nomination |
| GET | `/ai/match/{demandId}` | `AI_MATCH_VIEW` | Top-5 AI semantic matches |
| POST | `/ai/embed/engineer/{id}` | `AI_EMBEDDING_REFRESH` | Trigger embedding refresh |
| GET | `/demand-embeddings` | `DEMAND_EMBEDDING_VIEW` | List demand embeddings |
| POST | `/demand-embeddings` | `DEMAND_EMBEDDING_CREATE` | Create demand embedding |
| GET | `/demand-embeddings/{demandId}` | `DEMAND_EMBEDDING_VIEW` | Get embedding by demand |
| PUT | `/demand-embeddings/{demandId}` | `DEMAND_EMBEDDING_UPDATE` | Refresh demand embedding |
| DELETE | `/demand-embeddings/{demandId}` | `DEMAND_EMBEDDING_DELETE` | Delete demand embedding |
| POST | `/hris/import` | `HRIS_IMPORT` | Bulk CSV import (max 1000 rows) |
| GET | `/hris/import/{jobId}` | `HRIS_IMPORT_STATUS` | Get import job status |
| GET | `/analytics/match-summary` | `ANALYTICS_MATCH_VIEW` | Match success rate KPIs |
| GET | `/analytics/utilisation-summary` | `ANALYTICS_UTILISATION_VIEW` | Aggregated utilisation analytics |

---

## 2. Complete Permissions List (76 permissions)

| # | Permission | Domain |
|---|------------|--------|
| 1 | `AUTH_SELF` | Auth |
| 2 | `USER_VIEW` | User Management |
| 3 | `ROLE_ASSIGN` | RBAC |
| 4 | `ROLE_VIEW` | RBAC |
| 5 | `PERMISSION_VIEW` | RBAC |
| 6 | `PERMISSION_ASSIGN` | RBAC |
| 7 | `DEMAND_CREATE` | Demand |
| 8 | `DEMAND_VIEW` | Demand |
| 9 | `DEMAND_UPDATE` | Demand |
| 10 | `DEMAND_DELETE` | Demand |
| 11 | `DEMAND_APPROVE` | Demand Workflow |
| 12 | `DEMAND_PM_APPROVE` | Demand Workflow (PM-owned project) |
| 13 | `DEMAND_STATUS_TRANSITION` | Demand Workflow |
| 14 | `DEMAND_PIPELINE_VIEW` | Demand Workflow |
| 15 | `ANALYTICS_DEMAND_VIEW` | Analytics |
| 16 | `AUDIT_VIEW` | Governance |
| 17 | `NOTIFICATION_VIEW` | Notifications |
| 18 | `AI_SKILL_SUGGEST` | AI |
| 19 | `CANDIDATE_VIEW` | Candidate |
| 20 | `CANDIDATE_CREATE` | Candidate |
| 21 | `CANDIDATE_UPDATE` | Candidate |
| 22 | `CANDIDATE_NOTE_CREATE` | Candidate |
| 23 | `RESUME_UPLOAD` | Candidate |
| 24 | `AI_CANDIDATE_SCORE` | AI |
| 25 | `AI_REJECTION_EMAIL_GENERATE` | AI |
| 26 | `AI_REJECTION_EMAIL_SEND` | AI |
| 27 | `AI_INTERVIEW_QUESTIONS` | AI |
| 28 | `ASYNC_JOB_VIEW` | System |
| 29 | `APPLICATION_VIEW` | Application |
| 30 | `APPLICATION_CREATE` | Application |
| 31 | `APPLICATION_BULK_ACTION` | Application |
| 32 | `APPLICATION_STAGE_MOVE` | Application |
| 33 | `INTERVIEW_VIEW` | Interview |
| 34 | `INTERVIEW_SCHEDULE` | Interview |
| 35 | `INTERVIEW_CALENDAR_VIEW` | Interview |
| 36 | `INTERVIEW_UPDATE` | Interview |
| 37 | `SCORECARD_SUBMIT` | Scorecard |
| 38 | `SCORECARD_VIEW` | Scorecard |
| 39 | `OFFER_CREATE` | Offer |
| 40 | `OFFER_VIEW` | Offer |
| 41 | `OFFER_APPROVE` | Offer |
| 42 | `OFFER_REJECT` | Offer |
| 43 | `ANALYTICS_PIPELINE_VIEW` | Analytics |
| 44 | `GDPR_EXPORT` | GDPR |
| 45 | `GDPR_DELETE` | GDPR |
| 46 | `JOB_POSTING_VIEW` | Job Posting |
| 47 | `JOB_POSTING_CREATE` | Job Posting |
| 48 | `JOB_POSTING_UPDATE` | Job Posting |
| 49 | `JOB_POSTING_DELETE` | Job Posting |
| 50 | `JOB_POSTING_APPROVE` | Job Posting |
| 51 | `JOB_POSTING_PUBLISH` | Job Posting |
| 52 | `JOB_POSTING_UNPUBLISH` | Job Posting |
| 53 | `ANALYTICS_POSTING_VIEW` | Analytics |
| 54 | `BRANDING_UPDATE` | Branding |
| 55 | `REFERRAL_CREATE` | Referral |
| 56 | `AI_JD_GENERATE` | AI |
| 57 | `AI_CAREERS_CHAT` | AI |
| 58 | `AI_CHANNEL_RECOMMEND` | AI |
| 59 | `AI_INTERACTION_CREATE` | AI |
| 60 | `AI_INTERACTION_VIEW` | AI |
| 61 | `ENGINEER_VIEW` | Workforce |
| 62 | `ENGINEER_CREATE` | Workforce |
| 63 | `ENGINEER_UPDATE` | Workforce |
| 64 | `ENGINEER_DELETE` | Workforce |
| 65 | `ENGINEER_SELF_UPDATE` | Workforce |
| 66 | `ENGINEER_SEARCH` | Workforce |
| 67 | `BENCH_VIEW` | Workforce |
| 68 | `UTILISATION_VIEW` | Utilisation |
| 69 | `UTILISATION_CREATE` | Utilisation |
| 70 | `UTILISATION_UPDATE` | Utilisation |
| 71 | `UTILISATION_DELETE` | Utilisation |
| 72 | `UTILISATION_ALERTS_VIEW` | Utilisation |
| 73 | `NOMINATION_VIEW` | Nomination |
| 74 | `NOMINATION_CREATE` | Nomination |
| 75 | `NOMINATION_ACCEPT` | Nomination |
| 76 | `NOMINATION_REJECT` | Nomination |
| 77 | `AI_MATCH_VIEW` | AI |
| 78 | `AI_EMBEDDING_REFRESH` | AI |
| 79 | `DEMAND_EMBEDDING_VIEW` | Demand Embedding |
| 80 | `DEMAND_EMBEDDING_CREATE` | Demand Embedding |
| 81 | `DEMAND_EMBEDDING_UPDATE` | Demand Embedding |
| 82 | `DEMAND_EMBEDDING_DELETE` | Demand Embedding |
| 83 | `HRIS_IMPORT` | HRIS |
| 84 | `HRIS_IMPORT_STATUS` | HRIS |
| 85 | `ANALYTICS_MATCH_VIEW` | Analytics |
| 86 | `ANALYTICS_UTILISATION_VIEW` | Analytics |

---

## 3. Role → Permission Matrix

> Legend: ✅ = Full Access | 🔍 = Read/View Only | ⚡ = Scoped (own BU/own data)

### ADMIN — Full platform governance

| Permission | Access |
|------------|--------|
| `AUTH_SELF` | ✅ |
| `USER_VIEW` | ✅ |
| `ROLE_ASSIGN` | ✅ |
| `ROLE_VIEW` | ✅ |
| `PERMISSION_VIEW` | ✅ |
| `PERMISSION_ASSIGN` | ✅ |
| `DEMAND_VIEW` | ✅ |
| `DEMAND_CREATE` | ✅ |
| `DEMAND_UPDATE` | ✅ |
| `DEMAND_DELETE` | ✅ |
| `DEMAND_APPROVE` | ✅ |
| `DEMAND_STATUS_TRANSITION` | ✅ |
| `DEMAND_PIPELINE_VIEW` | ✅ |
| `ANALYTICS_DEMAND_VIEW` | ✅ |
| `AUDIT_VIEW` | ✅ |
| `NOTIFICATION_VIEW` | ✅ |
| `AI_SKILL_SUGGEST` | ✅ |
| `CANDIDATE_VIEW` | ✅ |
| `CANDIDATE_CREATE` | ✅ |
| `CANDIDATE_UPDATE` | ✅ |
| `APPLICATION_VIEW` | ✅ |
| `INTERVIEW_VIEW` | ✅ |
| `SCORECARD_VIEW` | ✅ |
| `OFFER_VIEW` | ✅ |
| `ANALYTICS_PIPELINE_VIEW` | ✅ |
| `GDPR_EXPORT` | ✅ |
| `GDPR_DELETE` | ✅ |
| `JOB_POSTING_VIEW` | ✅ |
| `JOB_POSTING_APPROVE` | ✅ |
| `BRANDING_UPDATE` | ✅ |
| `AI_INTERACTION_VIEW` | ✅ |
| `ENGINEER_VIEW` | ✅ |
| `ENGINEER_CREATE` | ✅ |
| `ENGINEER_UPDATE` | ✅ |
| `ENGINEER_DELETE` | ✅ |
| `BENCH_VIEW` | ✅ |
| `UTILISATION_VIEW` | ✅ |
| `UTILISATION_ALERTS_VIEW` | ✅ |
| `NOMINATION_VIEW` | ✅ |
| `HRIS_IMPORT` | ✅ |
| `HRIS_IMPORT_STATUS` | ✅ |
| `ANALYTICS_MATCH_VIEW` | ✅ |
| `ANALYTICS_UTILISATION_VIEW` | ✅ |
| `ANALYTICS_POSTING_VIEW` | ✅ |

---

### HIRING_MANAGER — Demand owner, hiring decisions

| Permission | Access | Scope |
|------------|--------|-------|
| `AUTH_SELF` | ✅ | |
| `NOTIFICATION_VIEW` | ✅ | |
| `DEMAND_CREATE` | ✅ | Own BU |
| `DEMAND_VIEW` | ⚡ | Own demands |
| `DEMAND_UPDATE` | ⚡ | Own demands |
| `DEMAND_DELETE` | ⚡ | Own drafts only |
| `DEMAND_APPROVE` | ✅ | Own BU demands |
| `DEMAND_STATUS_TRANSITION` | ⚡ | Own demands |
| `DEMAND_PIPELINE_VIEW` | ⚡ | Own demands |
| `ANALYTICS_DEMAND_VIEW` | ⚡ | Own BU |
| `AI_SKILL_SUGGEST` | ✅ | |
| `APPLICATION_VIEW` | ⚡ | Own demand only |
| `SCORECARD_VIEW` | ⚡ | Own demand only |
| `INTERVIEW_VIEW` | ⚡ | Own demand only |
| `OFFER_VIEW` | ⚡ | Own demand only |
| `OFFER_APPROVE` | ✅ | As chain approver |
| `OFFER_REJECT` | ✅ | As chain approver |
| `NOMINATION_VIEW` | ⚡ | Own demands |
| `NOMINATION_ACCEPT` | ✅ | Own demands |
| `NOMINATION_REJECT` | ✅ | Own demands (reason ≥ 20 chars) |
| `CANDIDATE_VIEW` | ⚡ | Own BU candidates |

---

### RESOURCE_MANAGER (RM/RMG) — Internal workforce allocation

| Permission | Access | Scope |
|------------|--------|-------|
| `AUTH_SELF` | ✅ | |
| `NOTIFICATION_VIEW` | ✅ | |
| `DEMAND_VIEW` | ✅ | Organization-wide |
| `DEMAND_PIPELINE_VIEW` | ✅ | |
| `ENGINEER_VIEW` | ✅ | Organization-wide |
| `ENGINEER_CREATE` | ✅ | |
| `ENGINEER_UPDATE` | ✅ | |
| `ENGINEER_DELETE` | ✅ | |
| `ENGINEER_SEARCH` | ✅ | |
| `BENCH_VIEW` | ✅ | |
| `UTILISATION_VIEW` | ✅ | |
| `UTILISATION_CREATE` | ✅ | |
| `UTILISATION_UPDATE` | ✅ | |
| `UTILISATION_DELETE` | ✅ | |
| `UTILISATION_ALERTS_VIEW` | ✅ | |
| `NOMINATION_VIEW` | ✅ | |
| `NOMINATION_CREATE` | ✅ | |
| `AI_MATCH_VIEW` | ✅ | |
| `AI_EMBEDDING_REFRESH` | ✅ | |
| `DEMAND_EMBEDDING_VIEW` | ✅ | |
| `DEMAND_EMBEDDING_CREATE` | ✅ | |
| `DEMAND_EMBEDDING_UPDATE` | ✅ | |
| `DEMAND_EMBEDDING_DELETE` | ✅ | |
| `ANALYTICS_MATCH_VIEW` | ✅ | |
| `ANALYTICS_UTILISATION_VIEW` | ✅ | |
| `HRIS_IMPORT` | ✅ | |
| `HRIS_IMPORT_STATUS` | ✅ | |
| `ANALYTICS_DEMAND_VIEW` | ✅ | |

---

### RECRUITER — External hiring pipeline

| Permission | Access | Scope |
|------------|--------|-------|
| `AUTH_SELF` | ✅ | |
| `NOTIFICATION_VIEW` | ✅ | |
| `DEMAND_VIEW` | ✅ | External-open demands |
| `CANDIDATE_VIEW` | ✅ | All candidates |
| `CANDIDATE_CREATE` | ✅ | |
| `CANDIDATE_UPDATE` | ✅ | |
| `CANDIDATE_NOTE_CREATE` | ✅ | |
| `RESUME_UPLOAD` | ✅ | |
| `AI_CANDIDATE_SCORE` | ✅ | |
| `AI_REJECTION_EMAIL_GENERATE` | ✅ | |
| `AI_REJECTION_EMAIL_SEND` | ✅ | |
| `AI_INTERVIEW_QUESTIONS` | ✅ | |
| `ASYNC_JOB_VIEW` | ✅ | |
| `APPLICATION_VIEW` | ✅ | All applications |
| `APPLICATION_CREATE` | ✅ | |
| `APPLICATION_BULK_ACTION` | ✅ | |
| `APPLICATION_STAGE_MOVE` | ✅ | |
| `INTERVIEW_VIEW` | ✅ | |
| `INTERVIEW_SCHEDULE` | ✅ | |
| `INTERVIEW_CALENDAR_VIEW` | ✅ | |
| `INTERVIEW_UPDATE` | ✅ | |
| `SCORECARD_VIEW` | ✅ | All scorecards |
| `OFFER_CREATE` | ✅ | |
| `OFFER_VIEW` | ✅ | |
| `OFFER_APPROVE` | ✅ | As chain approver |
| `ANALYTICS_PIPELINE_VIEW` | ✅ | |
| `JOB_POSTING_VIEW` | ✅ | |
| `JOB_POSTING_CREATE` | ✅ | |
| `JOB_POSTING_UPDATE` | ✅ | |
| `JOB_POSTING_DELETE` | ✅ | |
| `JOB_POSTING_PUBLISH` | ✅ | |
| `JOB_POSTING_UNPUBLISH` | ✅ | |
| `ANALYTICS_POSTING_VIEW` | ✅ | |
| `AI_JD_GENERATE` | ✅ | |
| `AI_CHANNEL_RECOMMEND` | ✅ | |
| `AI_INTERACTION_CREATE` | ✅ | Service-to-service |
| `GDPR_EXPORT` | ✅ | On behalf of candidate |
| `BRANDING_UPDATE` | ✅ | |

---

### INTERNAL_EMPLOYEE — Self-service only

| Permission | Access | Scope |
|------------|--------|-------|
| `AUTH_SELF` | ✅ | |
| `NOTIFICATION_VIEW` | ✅ | Own only |
| `ENGINEER_SELF_UPDATE` | ✅ | Own skills + availability |
| `ENGINEER_VIEW` | ⚡ | Own profile only |
| `NOMINATION_VIEW` | ⚡ | Own nominations only |
| `REFERRAL_CREATE` | ✅ | Per-posting referral links |

---

### CANDIDATE — External applicant (self-service)

| Permission | Access | Scope |
|------------|--------|-------|
| `AUTH_SELF` | ✅ | |
| `NOTIFICATION_VIEW` | ✅ | Own only |
| `CANDIDATE_VIEW` | ⚡ | Own profile only |
| `CANDIDATE_UPDATE` | ⚡ | Own profile only |
| `GDPR_EXPORT` | ⚡ | Own data only (DSAR) |
| `GDPR_DELETE` | ⚡ | Own data only |
| `OFFER_VIEW` | ⚡ | Own offers only |

> [!NOTE]
> Candidates also access **public endpoints** (no auth): `GET /careers`, `GET /careers/{slug}`, `POST /careers/{slug}/apply`, `GET /branding`, and `POST /ai/careers/chat`.

---

## 4. Consolidated Role × Permission Matrix (Summary View)

| Permission | ADMIN | HM | RM | RECRUITER | EMPLOYEE | CANDIDATE |
|------------|:-----:|:--:|:--:|:---------:|:--------:|:---------:|
| **Auth & Notifications** | | | | | | |
| `AUTH_SELF` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `NOTIFICATION_VIEW` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| **User & RBAC** | | | | | | |
| `USER_VIEW` | ✅ | | | | | |
| `ROLE_ASSIGN` | ✅ | | | | | |
| `ROLE_VIEW` | ✅ | | | | | |
| `PERMISSION_VIEW` | ✅ | | | | | |
| `PERMISSION_ASSIGN` | ✅ | | | | | |
| **Demand** | | | | | | |
| `DEMAND_CREATE` | ✅ | ✅ | | | | |
| `DEMAND_VIEW` | ✅ | ⚡ | ✅ | ✅ | | |
| `DEMAND_UPDATE` | ✅ | ⚡ | | | | |
| `DEMAND_DELETE` | ✅ | ⚡ | | | | |
| `DEMAND_APPROVE` | ✅ | ✅ | | | | |
| `DEMAND_STATUS_TRANSITION` | ✅ | ⚡ | | | | |
| `DEMAND_PIPELINE_VIEW` | ✅ | ⚡ | ✅ | | | |
| **Candidate** | | | | | | |
| `CANDIDATE_VIEW` | ✅ | ⚡ | | ✅ | | ⚡ |
| `CANDIDATE_CREATE` | ✅ | | | ✅ | | |
| `CANDIDATE_UPDATE` | | | | ✅ | | ⚡ |
| `CANDIDATE_NOTE_CREATE` | | | | ✅ | | |
| `RESUME_UPLOAD` | | | | ✅ | | |
| **Application** | | | | | | |
| `APPLICATION_VIEW` | ✅ | ⚡ | | ✅ | | |
| `APPLICATION_CREATE` | | | | ✅ | | |
| `APPLICATION_BULK_ACTION` | | | | ✅ | | |
| `APPLICATION_STAGE_MOVE` | | | | ✅ | | |
| **Interview** | | | | | | |
| `INTERVIEW_VIEW` | ✅ | ⚡ | | ✅ | | |
| `INTERVIEW_SCHEDULE` | | | | ✅ | | |
| `INTERVIEW_CALENDAR_VIEW` | | | | ✅ | | |
| `INTERVIEW_UPDATE` | | | | ✅ | | |
| **Scorecard** | | | | | | |
| `SCORECARD_SUBMIT` | | | | | | |
| `SCORECARD_VIEW` | ✅ | ⚡ | | ✅ | | |
| **Offer** | | | | | | |
| `OFFER_CREATE` | | | | ✅ | | |
| `OFFER_VIEW` | ✅ | ⚡ | | ✅ | | ⚡ |
| `OFFER_APPROVE` | | ✅ | | ✅ | | |
| `OFFER_REJECT` | | ✅ | | | | |
| **Job Posting** | | | | | | |
| `JOB_POSTING_VIEW` | ✅ | | | ✅ | | |
| `JOB_POSTING_CREATE` | | | | ✅ | | |
| `JOB_POSTING_UPDATE` | | | | ✅ | | |
| `JOB_POSTING_DELETE` | | | | ✅ | | |
| `JOB_POSTING_APPROVE` | ✅ | | | | | |
| `JOB_POSTING_PUBLISH` | | | | ✅ | | |
| `JOB_POSTING_UNPUBLISH` | | | | ✅ | | |
| `BRANDING_UPDATE` | ✅ | | | ✅ | | |
| `REFERRAL_CREATE` | | | | | ✅ | |
| **Workforce / Engineers** | | | | | | |
| `ENGINEER_VIEW` | ✅ | | ✅ | | ⚡ | |
| `ENGINEER_CREATE` | ✅ | | ✅ | | | |
| `ENGINEER_UPDATE` | ✅ | | ✅ | | | |
| `ENGINEER_DELETE` | ✅ | | ✅ | | | |
| `ENGINEER_SELF_UPDATE` | | | | | ✅ | |
| `ENGINEER_SEARCH` | | | ✅ | | | |
| `BENCH_VIEW` | ✅ | | ✅ | | | |
| **Utilisation** | | | | | | |
| `UTILISATION_VIEW` | ✅ | | ✅ | | | |
| `UTILISATION_CREATE` | | | ✅ | | | |
| `UTILISATION_UPDATE` | | | ✅ | | | |
| `UTILISATION_DELETE` | | | ✅ | | | |
| `UTILISATION_ALERTS_VIEW` | ✅ | | ✅ | | | |
| **Nomination** | | | | | | |
| `NOMINATION_VIEW` | ✅ | ⚡ | ✅ | | ⚡ | |
| `NOMINATION_CREATE` | | | ✅ | | | |
| `NOMINATION_ACCEPT` | | ✅ | | | | |
| `NOMINATION_REJECT` | | ✅ | | | | |
| **AI** | | | | | | |
| `AI_SKILL_SUGGEST` | ✅ | ✅ | | | | |
| `AI_CANDIDATE_SCORE` | | | | ✅ | | |
| `AI_REJECTION_EMAIL_GENERATE` | | | | ✅ | | |
| `AI_REJECTION_EMAIL_SEND` | | | | ✅ | | |
| `AI_INTERVIEW_QUESTIONS` | | | | ✅ | | |
| `AI_JD_GENERATE` | | | | ✅ | | |
| `AI_CAREERS_CHAT` | | | | ✅ | | |
| `AI_CHANNEL_RECOMMEND` | | | | ✅ | | |
| `AI_MATCH_VIEW` | | | ✅ | | | |
| `AI_EMBEDDING_REFRESH` | | | ✅ | | | |
| `AI_INTERACTION_CREATE` | | | | ✅ | | |
| `AI_INTERACTION_VIEW` | ✅ | | | | | |
| **GDPR** | | | | | | |
| `GDPR_EXPORT` | ✅ | | | ✅ | | ⚡ |
| `GDPR_DELETE` | ✅ | | | | | ⚡ |
| **HRIS & Embeddings** | | | | | | |
| `HRIS_IMPORT` | ✅ | | ✅ | | | |
| `HRIS_IMPORT_STATUS` | ✅ | | ✅ | | | |
| `DEMAND_EMBEDDING_VIEW` | | | ✅ | | | |
| `DEMAND_EMBEDDING_CREATE` | | | ✅ | | | |
| `DEMAND_EMBEDDING_UPDATE` | | | ✅ | | | |
| `DEMAND_EMBEDDING_DELETE` | | | ✅ | | | |
| **Analytics** | | | | | | |
| `ANALYTICS_DEMAND_VIEW` | ✅ | ⚡ | ✅ | | | |
| `ANALYTICS_PIPELINE_VIEW` | ✅ | | | ✅ | | |
| `ANALYTICS_POSTING_VIEW` | ✅ | | | ✅ | | |
| `ANALYTICS_MATCH_VIEW` | ✅ | | ✅ | | | |
| `ANALYTICS_UTILISATION_VIEW` | ✅ | | ✅ | | | |
| **Governance** | | | | | | |
| `AUDIT_VIEW` | ✅ | | | | | |
| `ASYNC_JOB_VIEW` | | | | ✅ | | |

> [!IMPORTANT]
> `SCORECARD_SUBMIT` is special — it's limited to the **assigned interviewer** for that specific interview. This is enforced at the service layer, not by role. An interviewer could be any authenticated user (HM, RM, or a panel member).

> [!NOTE]
> **Public endpoints** (no auth required) are not in this matrix: `POST /auth/login`, `POST /auth/refresh`, health probes, `GET /careers`, `GET /careers/{slug}`, `POST /careers/{slug}/apply`, `GET /branding`, `POST /webhooks/docusign`.
