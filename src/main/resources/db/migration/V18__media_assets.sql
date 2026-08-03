-- Uploaded images. Existing tables keep storing a plain URL string, so seeded
-- external URLs still work and nothing had to be migrated — but anything
-- uploaded through the admin now has a row here too, which is what makes
-- delete, storage cleanup, and "what is this file" possible at all.

CREATE TABLE media_assets (
    id                UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    /* Provider-independent identifier: a path for local disk, an object key
       for S3, a public_id for Cloudinary. The provider turns it into a URL. */
    storage_key       VARCHAR(500) NOT NULL,
    url               TEXT         NOT NULL,
    thumbnail_url     TEXT,
    original_filename VARCHAR(255) NOT NULL,
    content_type      VARCHAR(100) NOT NULL,
    size_bytes        BIGINT       NOT NULL,
    width             INTEGER      NOT NULL,
    height            INTEGER      NOT NULL,
    /* Free-text grouping ("gallery", "hotels") so the picker can be scoped
       without a second table. */
    folder            VARCHAR(80)  NOT NULL DEFAULT 'general',
    /* Which provider wrote it — a bucket migration can find rows still on
       local disk rather than guessing from the URL shape. */
    storage_provider  VARCHAR(30)  NOT NULL DEFAULT 'LOCAL',
    uploaded_by       UUID,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uq_media_assets_storage_key UNIQUE (storage_key),
    CONSTRAINT fk_media_assets_uploaded_by FOREIGN KEY (uploaded_by)
        REFERENCES admins (id) ON DELETE SET NULL,
    CONSTRAINT ck_media_assets_size CHECK (size_bytes > 0),
    CONSTRAINT ck_media_assets_dimensions CHECK (width > 0 AND height > 0),
    CONSTRAINT ck_media_assets_provider CHECK (storage_provider IN ('LOCAL', 'S3', 'CLOUDINARY'))
);

CREATE INDEX idx_media_assets_folder_created ON media_assets (folder, created_at DESC);
CREATE INDEX idx_media_assets_uploaded_by ON media_assets (uploaded_by) WHERE uploaded_by IS NOT NULL;
