-- Hotel inventory: the hotel itself, its media, its bookable room products,
-- and the amenity catalogue shared across all hotels.

-- Master catalogue. Normalised so "Free WiFi" is one row referenced by
-- thousands of hotels, rather than a free-text string repeated per hotel.
CREATE TABLE amenities (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name          VARCHAR(100) NOT NULL,
    slug          VARCHAR(120) NOT NULL,
    icon          VARCHAR(80),
    category      VARCHAR(30)  NOT NULL DEFAULT 'GENERAL',
    display_order INTEGER      NOT NULL DEFAULT 0,
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uq_amenities_name UNIQUE (name),
    CONSTRAINT uq_amenities_slug UNIQUE (slug),
    CONSTRAINT ck_amenities_slug_format CHECK (slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$'),
    CONSTRAINT ck_amenities_category CHECK (category IN
        ('GENERAL', 'ROOM', 'DINING', 'WELLNESS', 'BUSINESS', 'ACCESSIBILITY', 'FAMILY', 'OUTDOOR'))
);

CREATE TRIGGER trg_amenities_updated_at
    BEFORE UPDATE ON amenities
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


CREATE TABLE room_types (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name           VARCHAR(80)  NOT NULL,
    slug           VARCHAR(100) NOT NULL,
    description    TEXT,
    max_occupancy  SMALLINT     NOT NULL DEFAULT 2,
    display_order  INTEGER      NOT NULL DEFAULT 0,
    is_active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uq_room_types_name UNIQUE (name),
    CONSTRAINT uq_room_types_slug UNIQUE (slug),
    CONSTRAINT ck_room_types_slug_format   CHECK (slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$'),
    CONSTRAINT ck_room_types_max_occupancy CHECK (max_occupancy > 0)
);

CREATE TRIGGER trg_room_types_updated_at
    BEFORE UPDATE ON room_types
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


CREATE TABLE hotels (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name             VARCHAR(200) NOT NULL,
    slug             VARCHAR(220) NOT NULL,
    description      TEXT,
    short_description VARCHAR(500),
    star_rating      SMALLINT,
    city_id          UUID         NOT NULL,
    address_line1    VARCHAR(255) NOT NULL,
    address_line2    VARCHAR(255),
    postal_code      VARCHAR(20),
    latitude         NUMERIC(9,6),
    longitude        NUMERIC(9,6),
    contact_email    VARCHAR(255),
    contact_phone    VARCHAR(20),
    website_url      TEXT,
    check_in_time    TIME,
    check_out_time   TIME,
    base_price       NUMERIC(12,2) NOT NULL DEFAULT 0,
    currency_code    VARCHAR(3)      NOT NULL DEFAULT 'USD',
    rating_average   NUMERIC(3,2) NOT NULL DEFAULT 0,
    rating_count     INTEGER      NOT NULL DEFAULT 0,
    is_featured      BOOLEAN      NOT NULL DEFAULT FALSE,
    status           VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    meta_title       VARCHAR(255),
    meta_description VARCHAR(500),
    created_by       UUID,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at       TIMESTAMPTZ,

    -- Full-text search column maintained by Postgres, not the application,
    -- so it can never drift out of sync with the source columns.
    search_vector    tsvector GENERATED ALWAYS AS (
        setweight(to_tsvector('english'::regconfig, coalesce(name, '')), 'A') ||
        setweight(to_tsvector('english'::regconfig, coalesce(short_description, '')), 'B') ||
        setweight(to_tsvector('english'::regconfig, coalesce(description, '')), 'C')
    ) STORED,

    CONSTRAINT fk_hotels_city FOREIGN KEY (city_id)
        REFERENCES cities (id) ON DELETE RESTRICT,
    CONSTRAINT fk_hotels_created_by FOREIGN KEY (created_by)
        REFERENCES admins (id) ON DELETE SET NULL,
    CONSTRAINT ck_hotels_slug_format  CHECK (slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$'),
    CONSTRAINT ck_hotels_star_rating  CHECK (star_rating IS NULL OR star_rating BETWEEN 1 AND 5),
    CONSTRAINT ck_hotels_base_price   CHECK (base_price >= 0),
    CONSTRAINT ck_hotels_currency     CHECK (currency_code ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_hotels_status       CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    CONSTRAINT ck_hotels_rating_avg   CHECK (rating_average BETWEEN 0 AND 5),
    CONSTRAINT ck_hotels_rating_count CHECK (rating_count >= 0),
    CONSTRAINT ck_hotels_latitude     CHECK (latitude  IS NULL OR latitude  BETWEEN -90  AND 90),
    CONSTRAINT ck_hotels_longitude    CHECK (longitude IS NULL OR longitude BETWEEN -180 AND 180)
);

CREATE UNIQUE INDEX uq_hotels_slug_active ON hotels (slug) WHERE deleted_at IS NULL;
CREATE INDEX idx_hotels_city_id    ON hotels (city_id);
CREATE INDEX idx_hotels_created_by ON hotels (created_by);
CREATE INDEX idx_hotels_search     ON hotels USING GIN (search_vector);
CREATE INDEX idx_hotels_name_trgm  ON hotels USING GIN (name gin_trgm_ops);
-- Covers the public listing query: published hotels in a city, cheapest first.
CREATE INDEX idx_hotels_published_city_price ON hotels (city_id, base_price)
    WHERE status = 'PUBLISHED' AND deleted_at IS NULL;
CREATE INDEX idx_hotels_featured ON hotels (is_featured)
    WHERE is_featured AND status = 'PUBLISHED' AND deleted_at IS NULL;

CREATE TRIGGER trg_hotels_updated_at
    BEFORE UPDATE ON hotels
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


CREATE TABLE hotel_images (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    hotel_id      UUID        NOT NULL,
    url           TEXT        NOT NULL,
    alt_text      VARCHAR(255),
    caption       VARCHAR(500),
    display_order INTEGER     NOT NULL DEFAULT 0,
    is_cover      BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_hotel_images_hotel FOREIGN KEY (hotel_id)
        REFERENCES hotels (id) ON DELETE CASCADE
);

CREATE INDEX idx_hotel_images_hotel_id ON hotel_images (hotel_id, display_order);
-- At most one cover image per hotel, enforced by the database rather than
-- by application convention.
CREATE UNIQUE INDEX uq_hotel_images_one_cover ON hotel_images (hotel_id) WHERE is_cover;

CREATE TRIGGER trg_hotel_images_updated_at
    BEFORE UPDATE ON hotel_images
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- Junction table. Carries a surrogate UUID PK (project-wide convention and
-- JPA-friendly) with a uniqueness constraint doing the real work.
CREATE TABLE hotel_amenities (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    hotel_id   UUID        NOT NULL,
    amenity_id UUID        NOT NULL,
    notes      VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_hotel_amenities_hotel FOREIGN KEY (hotel_id)
        REFERENCES hotels (id) ON DELETE CASCADE,
    CONSTRAINT fk_hotel_amenities_amenity FOREIGN KEY (amenity_id)
        REFERENCES amenities (id) ON DELETE CASCADE,
    CONSTRAINT uq_hotel_amenities UNIQUE (hotel_id, amenity_id)
);

CREATE INDEX idx_hotel_amenities_amenity_id ON hotel_amenities (amenity_id);


CREATE TABLE hotel_rooms (
    id              UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    hotel_id        UUID          NOT NULL,
    room_type_id    UUID          NOT NULL,
    name            VARCHAR(150)  NOT NULL,
    description     TEXT,
    max_adults      SMALLINT      NOT NULL DEFAULT 2,
    max_children    SMALLINT      NOT NULL DEFAULT 0,
    bed_count       SMALLINT      NOT NULL DEFAULT 1,
    bed_type        VARCHAR(50),
    size_sqm        NUMERIC(6,2),
    price_per_night NUMERIC(12,2) NOT NULL,
    currency_code   VARCHAR(3)       NOT NULL DEFAULT 'USD',
    total_rooms     INTEGER       NOT NULL DEFAULT 1,
    is_active       BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT fk_hotel_rooms_hotel FOREIGN KEY (hotel_id)
        REFERENCES hotels (id) ON DELETE CASCADE,
    CONSTRAINT fk_hotel_rooms_room_type FOREIGN KEY (room_type_id)
        REFERENCES room_types (id) ON DELETE RESTRICT,
    CONSTRAINT uq_hotel_rooms_hotel_name UNIQUE (hotel_id, name),
    CONSTRAINT ck_hotel_rooms_max_adults   CHECK (max_adults > 0),
    CONSTRAINT ck_hotel_rooms_max_children CHECK (max_children >= 0),
    CONSTRAINT ck_hotel_rooms_bed_count    CHECK (bed_count > 0),
    CONSTRAINT ck_hotel_rooms_size         CHECK (size_sqm IS NULL OR size_sqm > 0),
    CONSTRAINT ck_hotel_rooms_price        CHECK (price_per_night >= 0),
    CONSTRAINT ck_hotel_rooms_total_rooms  CHECK (total_rooms >= 0),
    CONSTRAINT ck_hotel_rooms_currency     CHECK (currency_code ~ '^[A-Z]{3}$')
);

CREATE INDEX idx_hotel_rooms_hotel_id     ON hotel_rooms (hotel_id);
CREATE INDEX idx_hotel_rooms_room_type_id ON hotel_rooms (room_type_id);

CREATE TRIGGER trg_hotel_rooms_updated_at
    BEFORE UPDATE ON hotel_rooms
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
