-- Every page_seo row was seeded without an og_image_url, so every static route
-- shared as a bare link with no preview image — including the homepage, which is
-- the most-shared URL on any site. The mechanism worked; there was simply
-- nothing for it to emit.
--
-- These reuse images the site already shows on the pages themselves, sized to
-- 1200x630 (the ratio Facebook, LinkedIn, Slack and X all crop to). They are a
-- starting point an admin can replace from the CMS, not fixed values.

UPDATE page_seo SET og_image_url = 'https://images.unsplash.com/photo-1476514525535-07fb3b4ae5f1?w=1200&h=630&fit=crop'
 WHERE path = '/' AND og_image_url IS NULL;

UPDATE page_seo SET og_image_url = 'https://images.unsplash.com/photo-1566073771259-6a8506099945?w=1200&h=630&fit=crop'
 WHERE path = '/hotels' AND og_image_url IS NULL;

UPDATE page_seo SET og_image_url = 'https://images.unsplash.com/photo-1501785888041-af3ef285b470?w=1200&h=630&fit=crop'
 WHERE path = '/packages' AND og_image_url IS NULL;

UPDATE page_seo SET og_image_url = 'https://images.unsplash.com/photo-1499750310107-5fef28a66643?w=1200&h=630&fit=crop'
 WHERE path = '/blog' AND og_image_url IS NULL;

UPDATE page_seo SET og_image_url = 'https://images.unsplash.com/photo-1502920917128-1aa500764cbd?w=1200&h=630&fit=crop'
 WHERE path = '/gallery' AND og_image_url IS NULL;

UPDATE page_seo SET og_image_url = 'https://images.unsplash.com/photo-1423666639041-f56000c27a9a?w=1200&h=630&fit=crop'
 WHERE path = '/contact' AND og_image_url IS NULL;


-- The booking lookup shows someone their own reservation. It is not disallowed
-- in robots.txt on purpose — a crawler has to be able to fetch the page to read
-- the noindex this sets. It is also absent from the sitemap.
UPDATE page_seo SET no_index = TRUE WHERE path = '/bookings';
