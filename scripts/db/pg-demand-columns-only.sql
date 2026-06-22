-- Fix legacy `demands` boolean columns on ANY PostgreSQL (no pgvector required).
-- Use this when `CREATE EXTENSION vector` fails on your server (e.g. stock Homebrew Postgres).
-- For AI skill similarity you still need a DB with pgvector — see scripts/db/README.md.

ALTER TABLE demands ADD COLUMN IF NOT EXISTS bench_hiring boolean;
UPDATE demands SET bench_hiring = false WHERE bench_hiring IS NULL;
ALTER TABLE demands ALTER COLUMN bench_hiring SET DEFAULT false;
ALTER TABLE demands ALTER COLUMN bench_hiring SET NOT NULL;

ALTER TABLE demands ADD COLUMN IF NOT EXISTS is_filled boolean;
UPDATE demands SET is_filled = false WHERE is_filled IS NULL;
ALTER TABLE demands ALTER COLUMN is_filled SET DEFAULT false;
ALTER TABLE demands ALTER COLUMN is_filled SET NOT NULL;
