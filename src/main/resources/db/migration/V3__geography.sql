-- Geography reference data. Cities are the anchor every sellable entity
-- (hotels, packages, itinerary stops) locates itself against.

CREATE TABLE countries (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(100) NOT NULL,
    iso2            VARCHAR(2)      NOT NULL,
    iso3            VARCHAR(3)      NOT NULL,
    phone_code      VARCHAR(10),
    currency_code   VARCHAR(3),
    flag_emoji      VARCHAR(16),
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uq_countries_iso2 UNIQUE (iso2),
    CONSTRAINT uq_countries_iso3 UNIQUE (iso3),
    CONSTRAINT uq_countries_name UNIQUE (name),
    CONSTRAINT ck_countries_iso2_format     CHECK (iso2 ~ '^[A-Z]{2}$'),
    CONSTRAINT ck_countries_iso3_format     CHECK (iso3 ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_countries_currency_format CHECK (currency_code IS NULL OR currency_code ~ '^[A-Z]{3}$')
);

CREATE INDEX idx_countries_is_active ON countries (is_active) WHERE is_active;

CREATE TRIGGER trg_countries_updated_at
    BEFORE UPDATE ON countries
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


CREATE TABLE cities (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    country_id     UUID         NOT NULL,
    name           VARCHAR(120) NOT NULL,
    slug           VARCHAR(140) NOT NULL,
    state_province VARCHAR(120),
    latitude       NUMERIC(9,6),
    longitude      NUMERIC(9,6),
    timezone       VARCHAR(64),
    is_popular     BOOLEAN      NOT NULL DEFAULT FALSE,
    is_active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT fk_cities_country FOREIGN KEY (country_id)
        REFERENCES countries (id) ON DELETE RESTRICT,
    -- Slugs only need to be unique within a country: /us/springfield and
    -- /gb/springfield can coexist.
    CONSTRAINT uq_cities_country_slug UNIQUE (country_id, slug),
    CONSTRAINT ck_cities_slug_format CHECK (slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$'),
    CONSTRAINT ck_cities_latitude    CHECK (latitude  IS NULL OR latitude  BETWEEN -90  AND 90),
    CONSTRAINT ck_cities_longitude   CHECK (longitude IS NULL OR longitude BETWEEN -180 AND 180)
);

CREATE INDEX idx_cities_country_id ON cities (country_id);
CREATE INDEX idx_cities_is_popular ON cities (is_popular) WHERE is_popular;
CREATE INDEX idx_cities_name_trgm  ON cities USING GIN (name gin_trgm_ops);

CREATE TRIGGER trg_cities_updated_at
    BEFORE UPDATE ON cities
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
