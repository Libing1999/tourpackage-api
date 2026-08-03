-- Sellable tour packages and their day-by-day itinerary, media, and
-- inclusion/exclusion line items.

CREATE TABLE tour_packages (
    id                UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    title             VARCHAR(200)  NOT NULL,
    slug              VARCHAR(220)  NOT NULL,
    summary           VARCHAR(500),
    description       TEXT,
    country_id        UUID          NOT NULL,
    city_id           UUID          NOT NULL,
    duration_days     SMALLINT      NOT NULL,
    duration_nights   SMALLINT      NOT NULL,
    price             NUMERIC(12,2) NOT NULL,
    discount_price    NUMERIC(12,2),
    currency_code     VARCHAR(3)       NOT NULL DEFAULT 'USD',
    min_group_size    SMALLINT      NOT NULL DEFAULT 1,
    max_group_size    SMALLINT,
    difficulty_level  VARCHAR(20)   NOT NULL DEFAULT 'EASY',
    rating_average    NUMERIC(3,2)  NOT NULL DEFAULT 0,
    rating_count      INTEGER       NOT NULL DEFAULT 0,
    is_featured       BOOLEAN       NOT NULL DEFAULT FALSE,
    status            VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    meta_title        VARCHAR(255),
    meta_description  VARCHAR(500),
    created_by        UUID,
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    deleted_at        TIMESTAMPTZ,

    search_vector     tsvector GENERATED ALWAYS AS (
        setweight(to_tsvector('english'::regconfig, coalesce(title, '')), 'A') ||
        setweight(to_tsvector('english'::regconfig, coalesce(summary, '')), 'B') ||
        setweight(to_tsvector('english'::regconfig, coalesce(description, '')), 'C')
    ) STORED,

    CONSTRAINT fk_tour_packages_country FOREIGN KEY (country_id)
        REFERENCES countries (id) ON DELETE RESTRICT,
    CONSTRAINT fk_tour_packages_city FOREIGN KEY (city_id)
        REFERENCES cities (id) ON DELETE RESTRICT,
    CONSTRAINT fk_tour_packages_created_by FOREIGN KEY (created_by)
        REFERENCES admins (id) ON DELETE SET NULL,
    CONSTRAINT ck_tour_packages_slug_format CHECK (slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$'),
    CONSTRAINT ck_tour_packages_duration_days   CHECK (duration_days > 0),
    CONSTRAINT ck_tour_packages_duration_nights CHECK (duration_nights >= 0),
    CONSTRAINT ck_tour_packages_price           CHECK (price >= 0),
    CONSTRAINT ck_tour_packages_discount_price  CHECK (discount_price IS NULL OR (discount_price >= 0 AND discount_price <= price)),
    CONSTRAINT ck_tour_packages_currency        CHECK (currency_code ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_tour_packages_group_size      CHECK (min_group_size > 0 AND (max_group_size IS NULL OR max_group_size >= min_group_size)),
    CONSTRAINT ck_tour_packages_difficulty      CHECK (difficulty_level IN ('EASY', 'MODERATE', 'CHALLENGING', 'EXTREME')),
    CONSTRAINT ck_tour_packages_status          CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    CONSTRAINT ck_tour_packages_rating_avg      CHECK (rating_average BETWEEN 0 AND 5),
    CONSTRAINT ck_tour_packages_rating_count    CHECK (rating_count >= 0)
);

CREATE UNIQUE INDEX uq_tour_packages_slug_active ON tour_packages (slug) WHERE deleted_at IS NULL;
CREATE INDEX idx_tour_packages_country_id ON tour_packages (country_id);
CREATE INDEX idx_tour_packages_city_id    ON tour_packages (city_id);
CREATE INDEX idx_tour_packages_created_by ON tour_packages (created_by);
CREATE INDEX idx_tour_packages_search     ON tour_packages USING GIN (search_vector);
CREATE INDEX idx_tour_packages_title_trgm ON tour_packages USING GIN (title gin_trgm_ops);
CREATE INDEX idx_tour_packages_published_city_price ON tour_packages (city_id, price)
    WHERE status = 'PUBLISHED' AND deleted_at IS NULL;
CREATE INDEX idx_tour_packages_featured ON tour_packages (is_featured)
    WHERE is_featured AND status = 'PUBLISHED' AND deleted_at IS NULL;

CREATE TRIGGER trg_tour_packages_updated_at
    BEFORE UPDATE ON tour_packages
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


CREATE TABLE package_itinerary (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    package_id  UUID        NOT NULL,
    day_number  SMALLINT    NOT NULL,
    title       VARCHAR(200) NOT NULL,
    description TEXT,
    city_id     UUID,
    meals       VARCHAR(100),
    accommodation VARCHAR(200),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_package_itinerary_package FOREIGN KEY (package_id)
        REFERENCES tour_packages (id) ON DELETE CASCADE,
    CONSTRAINT fk_package_itinerary_city FOREIGN KEY (city_id)
        REFERENCES cities (id) ON DELETE SET NULL,
    CONSTRAINT uq_package_itinerary_day UNIQUE (package_id, day_number),
    CONSTRAINT ck_package_itinerary_day_number CHECK (day_number > 0)
);

CREATE INDEX idx_package_itinerary_package_id ON package_itinerary (package_id, day_number);

CREATE TRIGGER trg_package_itinerary_updated_at
    BEFORE UPDATE ON package_itinerary
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


CREATE TABLE package_images (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    package_id    UUID        NOT NULL,
    url           TEXT        NOT NULL,
    alt_text      VARCHAR(255),
    caption       VARCHAR(500),
    display_order INTEGER     NOT NULL DEFAULT 0,
    is_cover      BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_package_images_package FOREIGN KEY (package_id)
        REFERENCES tour_packages (id) ON DELETE CASCADE
);

CREATE INDEX idx_package_images_package_id ON package_images (package_id, display_order);
CREATE UNIQUE INDEX uq_package_images_one_cover ON package_images (package_id) WHERE is_cover;

CREATE TRIGGER trg_package_images_updated_at
    BEFORE UPDATE ON package_images
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


CREATE TABLE package_includes (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    package_id    UUID        NOT NULL,
    description   VARCHAR(300) NOT NULL,
    icon          VARCHAR(80),
    display_order INTEGER     NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_package_includes_package FOREIGN KEY (package_id)
        REFERENCES tour_packages (id) ON DELETE CASCADE
);

CREATE INDEX idx_package_includes_package_id ON package_includes (package_id, display_order);


CREATE TABLE package_excludes (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    package_id    UUID        NOT NULL,
    description   VARCHAR(300) NOT NULL,
    icon          VARCHAR(80),
    display_order INTEGER     NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_package_excludes_package FOREIGN KEY (package_id)
        REFERENCES tour_packages (id) ON DELETE CASCADE
);

CREATE INDEX idx_package_excludes_package_id ON package_excludes (package_id, display_order);
