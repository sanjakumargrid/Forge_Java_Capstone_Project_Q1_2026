-- One-time fixes for demand-service (pgvector + legacy `demands` columns).
-- Run as a superuser or DB owner if CREATE EXTENSION requires elevated rights.
--
-- If you see: ERROR extension "vector" is not available
--   → Your PostgreSQL binary does not ship pgvector (common for Homebrew / default installs).
--   → Use Docker Postgres from this repo (docker-compose.yml: pgvector/pgvector), then re-run,
--   → OR run only scripts/db/pg-demand-columns-only.sql on this DB (no vector; demand AI
--     features that need `skills.embedding` will still require a pgvector DB).
-- See scripts/db/README.md.
--
-- 1) pgvector: required for `skills.embedding vector(768)`.
CREATE EXTENSION IF NOT EXISTS vector;

-- 2) Legacy `demands` rows: NOT NULL columns added without a DB default fail on
--    `ALTER TABLE ... ADD COLUMN ... NOT NULL`. Normalize nullable columns then enforce NOT NULL.
ALTER TABLE demands ADD COLUMN IF NOT EXISTS bench_hiring boolean;
UPDATE demands SET bench_hiring = false WHERE bench_hiring IS NULL;
ALTER TABLE demands ALTER COLUMN bench_hiring SET DEFAULT false;
ALTER TABLE demands ALTER COLUMN bench_hiring SET NOT NULL;

ALTER TABLE demands ADD COLUMN IF NOT EXISTS is_filled boolean;
UPDATE demands SET is_filled = false WHERE is_filled IS NULL;
ALTER TABLE demands ALTER COLUMN is_filled SET DEFAULT false;
ALTER TABLE demands ALTER COLUMN is_filled SET NOT NULL;
