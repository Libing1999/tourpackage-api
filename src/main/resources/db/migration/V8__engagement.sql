-- Lead-generation and customer-communication tables. These are written by
-- anonymous site visitors (no user_id required) but link to a user/package
-- when the submitter is identifiable.

CREATE TABLE inquiries (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID,
    package_id   UUID,
    name         VARCHAR(150) NOT NULL,
    email        VARCHAR(255) NOT NULL,
    phone        VARCHAR(20),
    travel_date  DATE,
    party_size   SMALLINT,
    message      TEXT        NOT NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'NEW',
    assigned_to  UUID,
    responded_at TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_inquiries_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT fk_inquiries_package FOREIGN KEY (package_id)
        REFERENCES tour_packages (id) ON DELETE SET NULL,
    CONSTRAINT fk_inquiries_assigned_to FOREIGN KEY (assigned_to)
        REFERENCES admins (id) ON DELETE SET NULL,
    CONSTRAINT ck_inquiries_status CHECK (status IN ('NEW', 'IN_PROGRESS', 'RESOLVED', 'CLOSED')),
    CONSTRAINT ck_inquiries_party_size CHECK (party_size IS NULL OR party_size > 0)
);

CREATE INDEX idx_inquiries_user_id     ON inquiries (user_id) WHERE user_id IS NOT NULL;
CREATE INDEX idx_inquiries_package_id  ON inquiries (package_id) WHERE package_id IS NOT NULL;
CREATE INDEX idx_inquiries_assigned_to ON inquiries (assigned_to) WHERE assigned_to IS NOT NULL;
CREATE INDEX idx_inquiries_status_created ON inquiries (status, created_at DESC);

CREATE TRIGGER trg_inquiries_updated_at
    BEFORE UPDATE ON inquiries
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


CREATE TABLE contacts (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(150) NOT NULL,
    email      VARCHAR(255) NOT NULL,
    phone      VARCHAR(20),
    subject    VARCHAR(200),
    message    TEXT        NOT NULL,
    status     VARCHAR(20) NOT NULL DEFAULT 'NEW',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT ck_contacts_status CHECK (status IN ('NEW', 'READ', 'REPLIED'))
);

CREATE INDEX idx_contacts_status_created ON contacts (status, created_at DESC);

CREATE TRIGGER trg_contacts_updated_at
    BEFORE UPDATE ON contacts
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


CREATE TABLE newsletter_subscribers (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255) NOT NULL,
    is_active       BOOLEAN     NOT NULL DEFAULT TRUE,
    subscribed_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    unsubscribed_at TIMESTAMPTZ,

    CONSTRAINT uq_newsletter_subscribers_email UNIQUE (email),
    CONSTRAINT ck_newsletter_subscribers_active CHECK (
        (is_active AND unsubscribed_at IS NULL) OR
        (NOT is_active AND unsubscribed_at IS NOT NULL)
    )
);

CREATE INDEX idx_newsletter_subscribers_is_active ON newsletter_subscribers (is_active) WHERE is_active;
