-- Aligns the CMS-driven navigation, gallery, and site name with the Leh Ladakh
-- theme. V17 seeded a generic worldwide nav/gallery and V21 named the site
-- "Leh Ladakh Tours"; this migration sets the header/footer menus the site
-- should show, replaces the gallery with Ladakh images, and finalises the
-- brand name as "Tour Leh Ladakh".
--
-- Deletes are scoped to the exact rows V17 seeded, so CMS-added rows survive.
-- Images are free-licence Unsplash photos on images.unsplash.com (the only
-- host the web app allow-lists), each verified to resolve.

-- =====================================================================
-- 1. Header navigation — Home, Packages, Hotel, Offers, Gallery, Contact
-- =====================================================================
DELETE FROM nav_links WHERE nav_group = 'HEADER';

INSERT INTO nav_links (nav_group, label, href, display_order) VALUES
    ('HEADER', 'Home',     '/',                  1),
    ('HEADER', 'Packages', '/packages',          2),
    ('HEADER', 'Hotel',    '/hotels',            3),
    ('HEADER', 'Offers',   '/packages?offers=1', 4),
    ('HEADER', 'Gallery',  '/gallery',           5),
    ('HEADER', 'Contact',  '/contact',           6);

-- =====================================================================
-- 2. Footer navigation — kept in step with the header (no blog / bookings)
-- =====================================================================
DELETE FROM nav_links WHERE nav_group = 'FOOTER';

INSERT INTO nav_links (nav_group, label, href, display_order) VALUES
    ('FOOTER', 'Home',           '/',                  1),
    ('FOOTER', 'Packages',       '/packages',          2),
    ('FOOTER', 'Hotels',         '/hotels',            3),
    ('FOOTER', 'Special Offers', '/packages?offers=1', 4),
    ('FOOTER', 'Gallery',        '/gallery',           5),
    ('FOOTER', 'FAQ',            '/#faq',              6),
    ('FOOTER', 'Contact Us',     '/contact',           7);

-- =====================================================================
-- 3. Gallery — replace the demo world tour with Ladakh scenes
-- =====================================================================
DELETE FROM gallery_images WHERE caption IN (
    'Rome, Italy', 'Venice, Italy', 'Paris, France', 'Dubai, UAE',
    'Bali, Indonesia', 'Phuket, Thailand', 'Bangkok, Thailand', 'Yogyakarta, Indonesia');

INSERT INTO gallery_images (url, alt_text, caption, category, display_order) VALUES
    ('https://images.unsplash.com/photo-1660303954454-cc270a0d48c4?w=1200&h=900&fit=crop&auto=format&q=80',
     'Pangong Tso ringed by mountains', 'Pangong Lake', 'Lakes', 1),
    ('https://images.unsplash.com/photo-1583602415713-a0319a0188c0?w=1200&h=900&fit=crop&auto=format&q=80',
     'A high-plateau lake below brown peaks', 'Tso Moriri', 'Lakes', 2),
    ('https://images.unsplash.com/photo-1668966780008-b7dae98f61f3?w=1200&h=900&fit=crop&auto=format&q=80',
     'A Ladakhi town below the mountains', 'Leh', 'Towns', 3),
    ('https://images.unsplash.com/photo-1686993999944-f7fd395cdcf7?w=1200&h=900&fit=crop&auto=format&q=80',
     'The desert road into the Nubra Valley', 'Nubra Valley', 'Roads', 4),
    ('https://images.unsplash.com/photo-1760835251791-1fda687de791?w=1200&h=900&fit=crop&auto=format&q=80',
     'A monastery set among the hills', 'Sham Valley', 'Monasteries', 5),
    ('https://images.unsplash.com/photo-1782317341310-335b55dc6537?w=1200&h=900&fit=crop&auto=format&q=80',
     'A monastery on a green Himalayan slope', 'Turtuk', 'Monasteries', 6),
    ('https://images.unsplash.com/photo-1735909600958-f442498875f9?w=1200&h=900&fit=crop&auto=format&q=80',
     'A village deep in the Zanskar ranges', 'Zanskar Valley', 'Villages', 7),
    ('https://images.unsplash.com/photo-1757007813494-f9e7b880d2c6?w=1200&h=900&fit=crop&auto=format&q=80',
     'Prayer flags and an Indian flag above a lake', 'Pangong Tso', 'Lakes', 8);

-- =====================================================================
-- 4. Site name — final brand
-- =====================================================================
UPDATE settings SET value = 'Tour Leh Ladakh' WHERE key = 'site_name';
