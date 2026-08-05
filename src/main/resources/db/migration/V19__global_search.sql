-- Global search across hotels, packages, cities (destinations) and countries.
--
-- The searchable columns already carry the infrastructure this needs: hotels and
-- tour_packages have STORED weighted `search_vector` columns with GIN indexes
-- (V5, V6), and hotels.name / tour_packages.title / cities.name have trigram
-- indexes. None of it was ever queried — the existing search used
-- `LOWER(name) LIKE LOWER('%x%')`, which cannot use an index on `name` because
-- the index is on the column, not on the expression. Measured on 200k synthetic
-- rows: that shape is a 54.6ms parallel sequential scan reading 2062 buffers,
-- against 6.1ms and 528 buffers for `name ILIKE '%x%'`, which the trigram index
-- serves as a bitmap index scan.
--
-- So this migration adds only what is genuinely missing.

-- countries.name was the one searchable name column without a trigram index.
CREATE INDEX idx_countries_name_trgm ON countries USING GIN (name gin_trgm_ops);

-- cities.slug is how a destination is linked to from search results.
CREATE INDEX idx_cities_slug ON cities (slug);


-- What people actually searched for, aggregated rather than appended.
--
-- One row per distinct term with a counter, not one row per search: the only
-- question asked of this table is "what are the most common terms", and a log
-- would grow without bound to answer it. The cost is losing per-search history
-- (who, when, what they clicked) — if that is ever wanted it belongs in an
-- analytics pipeline, not here.
CREATE TABLE search_queries
(
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Lowercased and whitespace-collapsed, so "Bali", "bali" and "  BALI "
    -- are one row. This is the unique key.
    term             VARCHAR(100) NOT NULL,

    -- The last spelling a visitor actually typed, used for display. Showing
    -- "bali" as a suggestion when everyone typed "Bali" looks like a bug.
    display_term     VARCHAR(100) NOT NULL,

    search_count     BIGINT       NOT NULL DEFAULT 1,

    -- Lets a future ranking decay old terms; also makes a stale popular list
    -- diagnosable rather than mysterious.
    last_searched_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uq_search_queries_term UNIQUE (term),
    CONSTRAINT ck_search_queries_term_not_blank CHECK (length(btrim(term)) > 0),
    CONSTRAINT ck_search_queries_count_positive CHECK (search_count > 0)
);

-- The popular-searches query is "top N by count", so the index carries the
-- ordering. Partial: a term searched once is noise, never a suggestion.
CREATE INDEX idx_search_queries_popular ON search_queries (search_count DESC, last_searched_at DESC)
    WHERE search_count > 1;


-- Seed terms so "Popular searches" isn't empty on a fresh install. Counts are
-- deliberately low — real traffic overtakes them almost immediately, which is
-- the intent: these are a starting point, not a permanent fixture.
INSERT INTO search_queries (term, display_term, search_count)
VALUES ('bali', 'Bali', 12),
       ('dubai', 'Dubai', 9),
       ('beach resort', 'Beach resort', 7),
       ('paris', 'Paris', 6),
       ('honeymoon', 'Honeymoon', 5),
       ('thailand', 'Thailand', 4)
ON CONFLICT (term) DO NOTHING;
