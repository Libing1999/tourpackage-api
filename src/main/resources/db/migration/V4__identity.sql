-- Identity: back-office admins and public-facing customers are kept in
-- separate tables. They authenticate through different flows, carry
-- different attributes, and merging them would force every customer row to
-- carry nullable staff columns (and vice versa).

CREATE TABLE admins (
    id                    UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name             VARCHAR(150) NOT NULL,
    -- Always stored lowercase; the application normalizes on write.
    email                 VARCHAR(255) NOT NULL,
    password_hash         VARCHAR(255) NOT NULL,
    role                  VARCHAR(30)  NOT NULL DEFAULT 'ADMIN',
    phone                 VARCHAR(20),
    avatar_url            TEXT,
    is_active             BOOLEAN      NOT NULL DEFAULT TRUE,
    last_login_at         TIMESTAMPTZ,
    failed_login_attempts SMALLINT     NOT NULL DEFAULT 0,
    locked_until          TIMESTAMPTZ,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uq_admins_email UNIQUE (email),
    CONSTRAINT ck_admins_role CHECK (role IN ('SUPER_ADMIN', 'ADMIN', 'EDITOR', 'SUPPORT')),
    CONSTRAINT ck_admins_failed_attempts CHECK (failed_login_attempts >= 0)
);

CREATE INDEX idx_admins_is_active ON admins (is_active) WHERE is_active;

CREATE TRIGGER trg_admins_updated_at
    BEFORE UPDATE ON admins
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


CREATE TABLE users (
    id                    UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    first_name            VARCHAR(100) NOT NULL,
    last_name             VARCHAR(100) NOT NULL,
    -- Always stored lowercase; the application normalizes on write.
    email                 VARCHAR(255) NOT NULL,
    -- Nullable: social-login accounts never set a local password.
    password_hash         VARCHAR(255),
    phone                 VARCHAR(20),
    date_of_birth         DATE,
    gender                VARCHAR(20),
    nationality_country_id UUID,
    avatar_url            TEXT,
    email_verified_at     TIMESTAMPTZ,
    phone_verified_at     TIMESTAMPTZ,
    status                VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    last_login_at         TIMESTAMPTZ,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at            TIMESTAMPTZ,

    CONSTRAINT fk_users_nationality FOREIGN KEY (nationality_country_id)
        REFERENCES countries (id) ON DELETE SET NULL,
    CONSTRAINT ck_users_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED')),
    CONSTRAINT ck_users_gender CHECK (gender IS NULL OR gender IN ('MALE', 'FEMALE', 'OTHER', 'PREFER_NOT_TO_SAY')),
    CONSTRAINT ck_users_dob_past CHECK (date_of_birth IS NULL OR date_of_birth < CURRENT_DATE)
);

-- Partial unique index rather than a UNIQUE constraint: a soft-deleted user
-- must not block a new signup with the same address.
CREATE UNIQUE INDEX uq_users_email_active ON users (email) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_nationality_country_id ON users (nationality_country_id);
CREATE INDEX idx_users_status     ON users (status) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_created_at ON users (created_at DESC);

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
