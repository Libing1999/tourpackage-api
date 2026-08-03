-- Two additions needed for the public homepage's "Travel Blogs" and
-- "Popular Destinations" sections:
--   1. cities had no image — fine for a pure geography lookup, but a
--      destination card without a photo isn't a destination card.
--   2. blog_posts didn't exist at all; no entity in the original design
--      covered editorial content.

ALTER TABLE cities ADD COLUMN image_url TEXT;

CREATE TABLE blog_posts (
    id                UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    title             VARCHAR(200) NOT NULL,
    slug              VARCHAR(220) NOT NULL,
    excerpt           VARCHAR(500),
    content           TEXT         NOT NULL,
    cover_image_url   TEXT,
    category          VARCHAR(100) NOT NULL DEFAULT 'GENERAL',
    author_id         UUID,
    status            VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    published_at      TIMESTAMPTZ,
    read_time_minutes SMALLINT,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT fk_blog_posts_author FOREIGN KEY (author_id)
        REFERENCES admins (id) ON DELETE SET NULL,
    CONSTRAINT uq_blog_posts_slug UNIQUE (slug),
    CONSTRAINT ck_blog_posts_slug_format CHECK (slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$'),
    CONSTRAINT ck_blog_posts_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    CONSTRAINT ck_blog_posts_read_time CHECK (read_time_minutes IS NULL OR read_time_minutes > 0),
    CONSTRAINT ck_blog_posts_published_at CHECK (
        (status = 'PUBLISHED' AND published_at IS NOT NULL) OR (status != 'PUBLISHED')
    )
);

CREATE INDEX idx_blog_posts_author_id ON blog_posts (author_id) WHERE author_id IS NOT NULL;
-- Backs the homepage's "recent posts" query directly.
CREATE INDEX idx_blog_posts_published ON blog_posts (published_at DESC) WHERE status = 'PUBLISHED';

CREATE TRIGGER trg_blog_posts_updated_at
    BEFORE UPDATE ON blog_posts
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
