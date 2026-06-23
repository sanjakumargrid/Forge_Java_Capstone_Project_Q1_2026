-- Fix "auth_version" column missing or invalid after Hibernate ddl-auto=update
-- tried to add NOT NULL without a default on a non-empty `users` table.
--
-- Run once against your TalentGrid database (adjust host/port if needed):
--   psql "postgresql://talentgrid:talentgrid@localhost:5432/talentgrid" -f scripts/db/pg-users-auth-version.sql
-- Or for Docker Postgres on host port 5433:
--   psql "postgresql://talentgrid:talentgrid@localhost:5433/talentgrid" -f scripts/db/pg-users-auth-version.sql

ALTER TABLE users ADD COLUMN IF NOT EXISTS auth_version bigint;

UPDATE users SET auth_version = 1 WHERE auth_version IS NULL;

ALTER TABLE users ALTER COLUMN auth_version SET DEFAULT 1;

ALTER TABLE users ALTER COLUMN auth_version SET NOT NULL;
