-- Extensions and shared helpers used by every subsequent migration.
--
-- pg_trgm  : trigram GIN indexes powering fuzzy name/title search.
--
-- Email columns are plain VARCHAR, not citext: Hibernate's PostgreSQL
-- dialect doesn't recognize citext under ddl-auto=validate ("wrong column
-- type ... found [citext], expecting [varchar]"), which breaks schema
-- validation for every entity with an email field. Case-insensitivity is
-- instead handled by normalizing to lowercase at the application layer
-- before every read/write.
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Keeps updated_at accurate even for writes that bypass the application
-- (manual SQL, batch jobs, admin tooling). Attached per-table below.
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Human-facing booking references (TP-2026-000042) must be gapless-ish and
-- collision-free under concurrency; a sequence is the only safe source.
CREATE SEQUENCE IF NOT EXISTS booking_reference_seq START WITH 1 INCREMENT BY 1;
