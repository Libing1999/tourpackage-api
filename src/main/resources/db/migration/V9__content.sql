-- Marketing/CMS content: homepage banners, testimonials, FAQs, and a
-- generic key-value settings store for site-wide configuration.

CREATE TABLE banners (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    title         VARCHAR(200),
    subtitle      VARCHAR(300),
    image_url     TEXT        NOT NULL,
    link_url      TEXT,
    button_label  VARCHAR(50),
    display_order INTEGER     NOT NULL DEFAULT 0,
    is_active     BOOLEAN     NOT NULL DEFAULT TRUE,
    starts_at     TIMESTAMPTZ,
    ends_at       TIMESTAMPTZ,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT ck_banners_date_range CHECK (starts_at IS NULL OR ends_at IS NULL OR ends_at > starts_at)
);

CREATE INDEX idx_banners_active_order ON banners (display_order) WHERE is_active;

CREATE TRIGGER trg_banners_updated_at
    BEFORE UPDATE ON banners
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


CREATE TABLE testimonials (
    id                   UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id              UUID,
    package_id           UUID,
    customer_name        VARCHAR(150) NOT NULL,
    customer_avatar_url  TEXT,
    customer_country_id  UUID,
    rating               SMALLINT    NOT NULL,
    message              TEXT        NOT NULL,
    is_featured          BOOLEAN     NOT NULL DEFAULT FALSE,
    is_active            BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_testimonials_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT fk_testimonials_package FOREIGN KEY (package_id)
        REFERENCES tour_packages (id) ON DELETE SET NULL,
    CONSTRAINT fk_testimonials_country FOREIGN KEY (customer_country_id)
        REFERENCES countries (id) ON DELETE SET NULL,
    CONSTRAINT ck_testimonials_rating CHECK (rating BETWEEN 1 AND 5)
);

CREATE INDEX idx_testimonials_package_id ON testimonials (package_id) WHERE package_id IS NOT NULL;
CREATE INDEX idx_testimonials_featured   ON testimonials (is_featured) WHERE is_featured AND is_active;

CREATE TRIGGER trg_testimonials_updated_at
    BEFORE UPDATE ON testimonials
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


CREATE TABLE faqs (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    question      VARCHAR(300) NOT NULL,
    answer        TEXT        NOT NULL,
    category      VARCHAR(100) NOT NULL DEFAULT 'GENERAL',
    display_order INTEGER     NOT NULL DEFAULT 0,
    is_active     BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_faqs_category_order ON faqs (category, display_order) WHERE is_active;

CREATE TRIGGER trg_faqs_updated_at
    BEFORE UPDATE ON faqs
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


CREATE TABLE settings (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    key         VARCHAR(150) NOT NULL,
    value       TEXT,
    value_type  VARCHAR(20) NOT NULL DEFAULT 'STRING',
    group_name  VARCHAR(100) NOT NULL DEFAULT 'GENERAL',
    is_public   BOOLEAN     NOT NULL DEFAULT FALSE,
    updated_by  UUID,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_settings_key UNIQUE (key),
    CONSTRAINT fk_settings_updated_by FOREIGN KEY (updated_by)
        REFERENCES admins (id) ON DELETE SET NULL,
    CONSTRAINT ck_settings_value_type CHECK (value_type IN ('STRING', 'NUMBER', 'BOOLEAN', 'JSON'))
);

CREATE INDEX idx_settings_group_name ON settings (group_name);

CREATE TRIGGER trg_settings_updated_at
    BEFORE UPDATE ON settings
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
