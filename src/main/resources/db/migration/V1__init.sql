-- Baseline migration. Enables UUID generation for entity primary keys.
-- Feature migrations (domain tables) start at V2__.
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
