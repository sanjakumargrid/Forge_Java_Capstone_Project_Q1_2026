# Database helper scripts

## `pg-demand-pgvector-and-columns.sql`

Runs in two parts:

1. **`CREATE EXTENSION vector`** — only works if PostgreSQL was built or packaged **with pgvector** (the `vector` shared library on the server). The repo’s Docker image is **`pgvector/pgvector`** (see `docker-compose.yml`).

2. **`demands` column fixes** — works on any Postgres; safe to re-run.

### Error: `extension "vector" is not available`

Your server is **plain PostgreSQL** (for example Homebrew or Postgres.app) **without** the pgvector add-on. `CREATE EXTENSION` cannot install software that is not on the machine.

**What to do:**

- **Recommended for this project:** run Postgres from Compose (includes pgvector), then point **`DB_URL`** at that instance.

  ```bash
  # From repo root. If port 5432 is already your local Postgres, use a host port mapping:
  echo 'POSTGRES_HOST_PORT=5433' >> .env   # once
  docker compose up -d postgres
  ```

  Then use `jdbc:postgresql://localhost:5433/talentgrid` (or `5432` if Compose owns that port) and run:

  ```bash
  psql "postgresql://talentgrid:talentgrid@localhost:5433/talentgrid" \
    -f scripts/db/pg-demand-pgvector-and-columns.sql
  ```

- **If you intentionally keep using local Postgres on 5432** without pgvector: run only the **`demands`** part (vector-dependent features like `skills.embedding` will still fail until you use a pgvector-enabled DB):

  ```bash
  psql "postgresql://…" -f scripts/db/pg-demand-columns-only.sql
  ```

## `pg-users-auth-version.sql`

Fixes legacy `users.auth_version` when Hibernate `ddl-auto=update` conflicts with existing rows. See `scripts/e2e/README.md` troubleshooting.
