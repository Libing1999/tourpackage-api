-- Server-side state backing the auth module: email verification status on
-- admins, plus three token tables. Tokens are never stored raw — only a
-- SHA-256 hash — so a database leak alone can't be used to impersonate a
-- session, reset a password, or verify an account.

ALTER TABLE admins ADD COLUMN email_verified_at TIMESTAMPTZ;

CREATE TABLE refresh_tokens (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    admin_id         UUID        NOT NULL,
    token_hash       VARCHAR(64) NOT NULL,
    expires_at       TIMESTAMPTZ NOT NULL,
    revoked_at       TIMESTAMPTZ,
    replaced_by_id   UUID,
    created_by_ip    VARCHAR(45),
    user_agent       VARCHAR(255),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_refresh_tokens_admin FOREIGN KEY (admin_id)
        REFERENCES admins (id) ON DELETE CASCADE,
    CONSTRAINT fk_refresh_tokens_replaced_by FOREIGN KEY (replaced_by_id)
        REFERENCES refresh_tokens (id) ON DELETE SET NULL,
    CONSTRAINT uq_refresh_tokens_token_hash UNIQUE (token_hash)
);

CREATE INDEX idx_refresh_tokens_admin_id ON refresh_tokens (admin_id);
-- Fast "is this token still usable" lookup: unrevoked, unexpired rows only.
CREATE INDEX idx_refresh_tokens_active ON refresh_tokens (token_hash)
    WHERE revoked_at IS NULL;


CREATE TABLE password_reset_tokens (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    admin_id    UUID        NOT NULL,
    token_hash  VARCHAR(64) NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    used_at     TIMESTAMPTZ,
    created_by_ip VARCHAR(45),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_password_reset_tokens_admin FOREIGN KEY (admin_id)
        REFERENCES admins (id) ON DELETE CASCADE,
    CONSTRAINT uq_password_reset_tokens_token_hash UNIQUE (token_hash)
);

CREATE INDEX idx_password_reset_tokens_admin_id ON password_reset_tokens (admin_id);


CREATE TABLE email_verification_tokens (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    admin_id    UUID        NOT NULL,
    token_hash  VARCHAR(64) NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    verified_at TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_email_verification_tokens_admin FOREIGN KEY (admin_id)
        REFERENCES admins (id) ON DELETE CASCADE,
    CONSTRAINT uq_email_verification_tokens_token_hash UNIQUE (token_hash)
);

CREATE INDEX idx_email_verification_tokens_admin_id ON email_verification_tokens (admin_id);
