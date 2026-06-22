# Demand workflow E2E (curl / Postman)

These artifacts exercise the **HM draft → submit → PM approve** path through the **API gateway** (`/api/v1/...`), using seeded users from `user-auth-service` `DataInitializer`.

---

## 1. Prerequisites

Install and have available on your machine:

| Requirement | Why |
|-------------|-----|
| **JDK 17+** (or the version this repo uses) | Spring Boot services |
| **Maven 3.9+** | Build and `spring-boot:run` |
| **Docker Desktop** (or Docker Engine + Compose) | Postgres + Kafka from `docker-compose.yml` |
| **Redis** on `localhost:6379` | user-auth cache/blacklist; **gateway rate limiting** (will fail or error if Redis is down) |
| **Python 3** | `demand-workflow-via-gateway.sh` decodes JWT `sub` |
| **curl** | Shell script |
| **jq** (optional) | Pretty-print JSON in the shell script |
| **Postman** (optional) | Import the collection JSON |

---

## 2. One-time: environment variables

**JWT signing** must be the **same** secret on **user-auth** and **demand-service** so issued tokens validate on demand. Both services now share the **same default** in `application.properties` when `JWT_SECRET` is unset (fine for local IDE runs). For any real environment, set **`JWT_SECRET`** explicitly to a long random value on **both** services.

**user-auth** also requires **Google OAuth** properties at startup (even if you only use password login). Use dummy values for local dev.

From the **repository root**, in every terminal where you start JVM services (or put these in a repo-root `.env` file if you use one), set at least:

```bash
# Optional locally — defaults in application.properties match between auth and demand
# export JWT_SECRET='your-long-random-string'

# Required by user-auth application.properties (dummy OK for password-only login)
export GOOGLE_CLIENT_ID='local-dummy'
export GOOGLE_CLIENT_SECRET='local-dummy'
```

**demand-service** uses Google Gemini for embeddings and optional LLM skill suggestions. For local startup without calling Gemini, you can leave **`GEMINI_API_KEY`** unset (the property defaults to empty and AI calls short-circuit). Set a real key in `.env` or the environment when you want embeddings and suggestions to work.

Optional overrides (defaults match Docker Compose Postgres):

```bash
export DB_URL='jdbc:postgresql://localhost:5432/talentgrid'
export DB_USERNAME='talentgrid'
export DB_PASSWORD='talentgrid'
export KAFKA_BOOTSTRAP_SERVERS='localhost:9092'
export REDIS_HOST='localhost'
export REDIS_PORT='6379'
```

**Gateway** uses Redis for the default **RequestRateLimiter**. Start Redis **before** the gateway.

---

## 3. Start Postgres + Kafka (Docker Compose)

From the **repository root** (the directory that contains `docker-compose.yml`):

```bash
cd "/path/to/Forge_Java_Capstone_Project_Q1_2026_Services 2"
docker compose up -d
```

Wait until containers are healthy (first run can take a minute):

```bash
docker compose ps
```

### Port 5432 already in use (common on macOS with local Postgres)

If you see **`Bind for 0.0.0.0:5432 failed: port is already allocated`**, another process is bound to **5432**. Do **not** stop it unless you intend to; instead map the container to another host port.

1. In the **repository root**, create or edit a **`.env`** file (Compose reads it automatically for variable substitution):

   ```bash
   echo 'POSTGRES_HOST_PORT=5433' >> .env
   ```

2. Bring Postgres up again:

   ```bash
   docker compose up -d postgres
   ```

3. Point **user-auth** and **demand-service** at the new port (Spring defaults still use 5432):

   ```bash
   export DB_URL='jdbc:postgresql://localhost:5433/talentgrid'
   ```

Defaults from `docker-compose.yml`:

| Service | Host | Port | Notes |
|---------|------|------|--------|
| PostgreSQL | `localhost` | **`${POSTGRES_HOST_PORT:-5432}`** (default **5432**) | Database `talentgrid`, user/password `talentgrid` |
| Kafka (KRaft) | `localhost` | `9092` | |
| Kafka UI | `localhost` | `7777` | Optional browser UI |

---

## 4. Start Redis on port 6379

If you do not already have Redis listening on `localhost:6379`, start one (example using Docker):

```bash
docker run -d --name talentgrid-redis -p 6379:6379 redis:7-alpine
```

Verify (pick one):

```bash
# If redis-cli is installed on the host:
redis-cli -h 127.0.0.1 -p 6379 ping

# If "command not found: redis-cli" — use the CLI inside the container:
docker exec talentgrid-redis redis-cli ping
# Expect: PONG
```

---

## 5. Build the three services (optional but recommended)

From **repository root**:

```bash
export JWT_SECRET='...'
export GOOGLE_CLIENT_ID='local-dummy'
export GOOGLE_CLIENT_SECRET='local-dummy'

mvn -pl services/user-auth-service,services/demand-service,talentgrid-api-gateway-service -am package -DskipTests
```

To compile and run tests instead, omit `-DskipTests` (slower).

---

## 6. Start the JVM services (order matters)

Use **three separate terminals**, all from **repository root**, with **`JWT_SECRET`, `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`** exported in each (see section 2).

### Terminal A — user-auth-service (port **8081**)

```bash
mvn -pl services/user-auth-service spring-boot:run
```

Wait for a log line similar to: started `UserAuthServiceApplication` on port **8081**.  
On first DB connect, `DataInitializer` seeds roles/users (including HM and PM).

### Terminal B — demand-service (port **8082**)

```bash
export JWT_SECRET='...same as user-auth...'
mvn -pl services/demand-service spring-boot:run
```

Wait for: started on port **8082**, Kafka consumer subscribed if applicable.

### Terminal C — API gateway (port **8080**)

Ensure **Redis** is up (section 4). Then:

```bash
mvn -pl talentgrid-api-gateway-service spring-boot:run
```

Wait for: gateway listening on **8080**.

**Port summary**

| Process | URL |
|---------|-----|
| Gateway | `http://localhost:8080` |
| user-auth | `http://localhost:8081` |
| demand | `http://localhost:8082` |

Do **not** run another process on 8080/8081/8082 while these are up.

---

## 7. Run the curl E2E script

From **repository root**:

```bash
chmod +x scripts/e2e/demand-workflow-via-gateway.sh   # once
./scripts/e2e/demand-workflow-via-gateway.sh
```

Useful overrides:

| Variable | Default | Meaning |
|----------|---------|---------|
| `GATEWAY_URL` | `http://localhost:8080` | Gateway base URL |
| `HM_EMAIL` / `HM_PASSWORD` | `hm@griddynamics.com` / `Password@123` | Hiring manager login |
| `PM_EMAIL` / `PM_PASSWORD` | `projectmanager@griddynamics.com` / `Password@123` | PM login |
| `PROJECT_ID` | `1` | Demand `projectId` in create payload |
| `ACCOUNT_ID` | `1` | Demand `accountId` in create payload |

Example:

```bash
GATEWAY_URL=http://localhost:8080 PROJECT_ID=42 ./scripts/e2e/demand-workflow-via-gateway.sh
```

The script prints HTTP bodies and the **HTTP status** for the approve call.

---

## 8. Run the same flow in Postman

1. Open Postman → **Import** → choose `scripts/e2e/E2E-Demand-Workflow.postman_collection.json`.
2. Open the collection → **Variables** tab.
3. Set at least:
   - `gatewayUrl` = `http://localhost:8080`
   - `hmEmail`, `hmPassword`, `pmEmail`, `pmPassword` (defaults match seed users)
   - `projectId`, `accountId` (defaults `1`)
4. Run the folder **in order** (requests are numbered 1–8):
   - **1–2** log in and store `hmToken` / `pmToken` in collection variables.
   - **3–4** load job title and skill ids into variables.
   - **5–8** create demand, submit, approve, get demand.

---

## 9. Why “approve” may return 403 (project / PM check)

`POST /api/v1/demands/{id}/approve` requires:

1. A valid **PM** JWT with scope **`DEMAND_PM_APPROVE`** (seeded PM user has this).
2. demand-service calls **user-auth** with Feign: **`GET /api/v1/projects/{projectId}`** and expects `projectManagerId` to equal the PM user’s id (JWT claim **`sub`**, same as user id in the database).

If user-auth does **not** implement `GET /api/v1/projects/{id}` yet, or the row for `PROJECT_ID` does not exist / `projectManagerId` does not match the PM user, demand-service returns **403** with a message about project ownership.

**What still works without that API:** login, lookups, create demand, submit (steps up to approve). **Approve** succeeds only once project data in user-auth matches the PM.

---

## 10. Stop everything

- Stop each Spring Boot process with **Ctrl+C** in its terminal.
- Infra:

```bash
docker compose down
docker stop talentgrid-redis 2>/dev/null || true
```

---

## Seeded credentials (`DataInitializer`)

| Role            | Email                             | Password       |
|-----------------|-----------------------------------|----------------|
| Hiring Manager  | `hm@griddynamics.com`             | `Password@123` |
| Project Manager | `projectmanager@griddynamics.com` | `Password@123` |

---

## Files in this folder

| File | Purpose |
|------|---------|
| `demand-workflow-via-gateway.sh` | bash + `curl` + `python3` (+ optional `jq`) |
| `E2E-Demand-Workflow.postman_collection.json` | Postman collection |

---

## Gateway path mapping

The gateway rewrites external **`/api/v1/...`** URLs to the paths each Spring service mounts (for example `/api/v1/demands/**`, `/api/v1/auth/**`). See `talentgrid-api-gateway-service/src/main/resources/application.yml`.

---

## Troubleshooting: user-auth fails on `auth_version` / `users`

If Hibernate logged **`column "auth_version" of relation "users" contains null values`** or **`column "auth_version" ... does not exist`**, an older `users` table conflicted with `ddl-auto=update` adding a NOT NULL column without a DB default.

**One-time fix:** run the SQL script against the same database your auth service uses:

```bash
psql "postgresql://talentgrid:talentgrid@localhost:5432/talentgrid" -f scripts/db/pg-users-auth-version.sql
```

(Use port **5433** in the URL if you set `POSTGRES_HOST_PORT=5433`.)

Then restart **user-auth-service**. The `User` entity now uses `@ColumnDefault("1")` so future schema updates are safer on PostgreSQL.

---

## Troubleshooting: demand-service — `vector` type, `skills` table, `bench_hiring` / `is_filled`

**`ERROR: type "vector" does not exist`**

PostgreSQL must have the **pgvector** extension. The repo’s `docker-compose.yml` uses a **pgvector** image; point **`DB_URL`** at that instance (or install pgvector on your server), then ensure the extension exists:

```bash
psql "postgresql://talentgrid:talentgrid@localhost:5432/talentgrid" -c "CREATE EXTENSION IF NOT EXISTS vector;"
```

**`column "bench_hiring" ... contains null values`** / **`is_filled`** when Hibernate runs `ddl-auto=update`

Existing `demands` rows need defaults before NOT NULL. Run:

```bash
psql "postgresql://talentgrid:talentgrid@localhost:5432/talentgrid" -f scripts/db/pg-demand-pgvector-and-columns.sql
```

(Adjust host/port/user/database to match your **`DB_URL`**.)

The `Demand` entity uses `@ColumnDefault("false")` on these booleans so new DDL from Hibernate is safer. If **`skills`** was never created because of the vector error, fix the extension (and run the script above), then restart demand-service so Hibernate can create **`skills`** and foreign keys.

**`Could not resolve placeholder 'GEMINI_API_KEY'`**

Use a repo-root `.env` with `GEMINI_API_KEY=...`, or rely on the default in `application.properties` (`gemini.api.key=${GEMINI_API_KEY:}`) so the app starts without the variable; AI features need a real key.

---

## Automated tests (no Docker required for unit tests)

From repo root:

```bash
mvn -pl services/demand-service,services/user-auth-service,talentgrid-api-gateway-service -am test
```

This validates compilation and module tests; it is **not** a substitute for the full Docker + Redis + three-services run above.
