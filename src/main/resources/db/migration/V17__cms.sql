-- CMS tables. Everything seeded here is a value that was previously hardcoded
-- in the frontend — section headings, nav labels, page metadata — so the site
-- renders identically after this migration, but from the database.

-- ---------------------------------------------------------------------
-- Banners already existed for the homepage slider. `placement` lets the same
-- table serve the slider, the offers strip, and per-page hero images, rather
-- than three near-identical tables.
-- ---------------------------------------------------------------------
ALTER TABLE banners ADD COLUMN placement VARCHAR(30) NOT NULL DEFAULT 'HOME_SLIDER';
ALTER TABLE banners ADD CONSTRAINT ck_banners_placement
    CHECK (placement IN ('HOME_SLIDER', 'OFFERS_STRIP', 'PAGE_HERO'));

DROP INDEX idx_banners_active_order;
CREATE INDEX idx_banners_placement_order ON banners (placement, display_order) WHERE is_active;


-- ---------------------------------------------------------------------
-- Section headings and page copy. Addressed by a dotted key rather than a
-- row per column so a new section is a new row, not a migration.
-- ---------------------------------------------------------------------
CREATE TABLE content_blocks (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    key         VARCHAR(100) NOT NULL,
    eyebrow     VARCHAR(100),
    title       VARCHAR(200),
    subtitle    VARCHAR(500),
    body        TEXT,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    updated_by  UUID,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uq_content_blocks_key UNIQUE (key),
    CONSTRAINT fk_content_blocks_updated_by FOREIGN KEY (updated_by)
        REFERENCES admins (id) ON DELETE SET NULL,
    CONSTRAINT ck_content_blocks_key_format CHECK (key ~ '^[a-z0-9]+(\.[a-z0-9-]+)*$')
);

CREATE TRIGGER trg_content_blocks_updated_at
    BEFORE UPDATE ON content_blocks
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- ---------------------------------------------------------------------
-- Per-route SEO. `path` is the Next.js route, so a page's metadata is edited
-- by the URL an admin already knows.
-- ---------------------------------------------------------------------
CREATE TABLE page_seo (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    path             VARCHAR(200) NOT NULL,
    meta_title       VARCHAR(200) NOT NULL,
    meta_description VARCHAR(500),
    og_image_url     TEXT,
    /* Detail pages derive their own title from the entity, so only the
       listing/static routes are managed here. */
    no_index         BOOLEAN      NOT NULL DEFAULT FALSE,
    updated_by       UUID,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uq_page_seo_path UNIQUE (path),
    CONSTRAINT fk_page_seo_updated_by FOREIGN KEY (updated_by)
        REFERENCES admins (id) ON DELETE SET NULL,
    CONSTRAINT ck_page_seo_path_format CHECK (path ~ '^/')
);

CREATE TRIGGER trg_page_seo_updated_at
    BEFORE UPDATE ON page_seo
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- ---------------------------------------------------------------------
-- Navbar and footer links. Previously plain arrays in the components, which
-- the README called out as the one deliberate exception to "nothing is
-- hardcoded" — this removes that exception.
-- ---------------------------------------------------------------------
CREATE TABLE nav_links (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    nav_group     VARCHAR(20)  NOT NULL,
    label         VARCHAR(80)  NOT NULL,
    href          VARCHAR(300) NOT NULL,
    display_order INTEGER      NOT NULL DEFAULT 0,
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT ck_nav_links_group CHECK (nav_group IN ('HEADER', 'FOOTER'))
);

CREATE INDEX idx_nav_links_group_order ON nav_links (nav_group, display_order) WHERE is_active;

CREATE TRIGGER trg_nav_links_updated_at
    BEFORE UPDATE ON nav_links
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- ---------------------------------------------------------------------
-- Gallery
-- ---------------------------------------------------------------------
CREATE TABLE gallery_images (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    url           TEXT         NOT NULL,
    alt_text      VARCHAR(255),
    caption       VARCHAR(300),
    category      VARCHAR(80)  NOT NULL DEFAULT 'General',
    display_order INTEGER      NOT NULL DEFAULT 0,
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_gallery_images_order ON gallery_images (category, display_order) WHERE is_active;

CREATE TRIGGER trg_gallery_images_updated_at
    BEFORE UPDATE ON gallery_images
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- ---------------------------------------------------------------------
-- Seed: the exact strings the components used to hold.
-- ---------------------------------------------------------------------
INSERT INTO content_blocks (key, eyebrow, title, subtitle) VALUES
    ('home.destinations', 'Where to next', 'Popular Destinations',
     'Handpicked places our travellers love most.'),
    ('home.hotels', 'Stay in style', 'Top Rated Hotels',
     'Five-star stays and boutique gems, rated by real travellers.'),
    ('home.packages', 'Fan favourites', 'Best Tour Packages',
     'Curated itineraries with everything arranged for you.'),
    ('home.offers', 'Limited time', 'Special Offers',
     'Save on these trips while the deals last.'),
    ('home.testimonials', 'Traveller stories', 'What Our Travellers Say',
     'Real reviews from people who booked with us.'),
    ('home.blog', 'From the blog', 'Travel Inspiration',
     'Guides, tips, and stories from the road.'),
    ('home.faq', 'Need help?', 'Frequently Asked Questions',
     'Answers to the questions we hear most.'),
    ('home.newsletter', NULL, 'Get Travel Deals in Your Inbox',
     'Join our newsletter for exclusive offers, new destinations, and travel tips.'),
    ('page.hotels', NULL, 'Find Your Hotel', NULL),
    ('page.packages', NULL, 'Tour Packages', NULL),
    ('page.contact', NULL, 'Get in touch',
     'Planning a trip or just have a question? Send us a message and we''ll get back to you — usually within one business day.'),
    ('page.gallery', 'Moments', 'Travel Gallery',
     'Photographs from the places we send people.'),
    ('page.bookings', NULL, 'Find your booking',
     'Enter the reference from your confirmation email, along with the email address you booked with.'),
    ('footer.tagline', NULL, NULL, 'Your journey, perfectly planned.');

INSERT INTO page_seo (path, meta_title, meta_description) VALUES
    ('/', 'TourPackage — Handpicked Destinations, Hotels & Tour Packages',
     'Discover curated travel packages, top-rated hotels, and unforgettable destinations. Book your next trip with TourPackage.'),
    ('/hotels', 'Hotels — Find & Book Your Perfect Stay',
     'Browse handpicked hotels around the world. Filter by price, star rating, and amenities to find your perfect stay.'),
    ('/packages', 'Tour Packages — Curated Trips & Holiday Deals',
     'Browse handpicked tour packages worldwide. Filter by price, duration, and difficulty to find your next trip.'),
    ('/contact', 'Contact Us',
     'Get in touch with the TourPackage team — plan a trip, ask a question, or find our office.'),
    ('/bookings', 'Find Your Booking',
     'Look up an existing TourPackage booking with your reference number and email.'),
    ('/gallery', 'Travel Gallery',
     'Photographs from the destinations, hotels, and tours we offer.'),
    ('/blog', 'Travel Blog',
     'Guides, tips, and stories from the road.');

INSERT INTO nav_links (nav_group, label, href, display_order) VALUES
    ('HEADER', 'Destinations', '/#destinations', 1),
    ('HEADER', 'Hotels',       '/hotels',        2),
    ('HEADER', 'Packages',     '/packages',      3),
    ('HEADER', 'Offers',       '/packages?offers=1', 4),
    ('HEADER', 'Gallery',      '/gallery',       5),
    ('HEADER', 'Blog',         '/blog',          6),
    ('HEADER', 'My Booking',   '/bookings',      7),
    ('HEADER', 'Contact',      '/contact',       8),
    ('FOOTER', 'Destinations',     '/#destinations',     1),
    ('FOOTER', 'Hotels',           '/hotels',            2),
    ('FOOTER', 'Packages',         '/packages',          3),
    ('FOOTER', 'Special Offers',   '/packages?offers=1', 4),
    ('FOOTER', 'Travel Gallery',   '/gallery',           5),
    ('FOOTER', 'Travel Blog',      '/blog',              6),
    ('FOOTER', 'FAQ',              '/#faq',              7),
    ('FOOTER', 'Find My Booking',  '/bookings',          8),
    ('FOOTER', 'Contact Us',       '/contact',           9);

-- Gallery seed reuses images already verified as depicting the right place
-- (see the note in tourpackage-web's README about content-checking, not just
-- link-checking, these URLs).
INSERT INTO gallery_images (url, alt_text, caption, category, display_order) VALUES
    ('https://images.unsplash.com/photo-1552832230-c0197dd311b5?w=1200&h=900&fit=crop&auto=format&q=80',
     'Rome street at golden hour', 'Rome, Italy', 'Cities', 1),
    ('https://images.unsplash.com/photo-1523906834658-6e24ef2386f9?w=1200&h=900&fit=crop&auto=format&q=80',
     'Venice canal', 'Venice, Italy', 'Cities', 2),
    ('https://images.unsplash.com/photo-1502602898657-3e91760cbb34?w=1200&h=900&fit=crop&auto=format&q=80',
     'Paris skyline', 'Paris, France', 'Cities', 3),
    ('https://images.unsplash.com/photo-1512453979798-5ea266f8880c?w=1200&h=900&fit=crop&auto=format&q=80',
     'Dubai skyline at dusk', 'Dubai, UAE', 'Cities', 4),
    ('https://images.unsplash.com/photo-1544644181-1484b3fdfc62?w=1200&h=900&fit=crop&auto=format&q=80',
     'Bali temple by the water', 'Bali, Indonesia', 'Nature', 5),
    ('https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=1200&h=900&fit=crop&auto=format&q=80',
     'Tropical beach', 'Phuket, Thailand', 'Beaches', 6),
    ('https://images.unsplash.com/photo-1508009603885-50cf7c579365?w=1200&h=900&fit=crop&auto=format&q=80',
     'Bangkok street at night', 'Bangkok, Thailand', 'Cities', 7),
    ('https://images.unsplash.com/photo-1596402184320-417e7178b2cd?w=1200&h=900&fit=crop&auto=format&q=80',
     'Borobudur temple at sunrise', 'Yogyakarta, Indonesia', 'Nature', 8);
