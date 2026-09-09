-- Re-themes the public site around Leh Ladakh. V13–V16 seeded a generic
-- worldwide demo (Bangkok, Paris, Dubai, …); this migration removes that
-- specific demo set and replaces it with a coherent Ladakh catalogue so the
-- homepage, listings, and detail pages all tell one story.
--
-- All copy here is original. Images are free-licence Unsplash photos, the same
-- source and URL convention V13 used; only images.unsplash.com is allow-listed
-- in the web app's next.config, so every image URL points there.
--
-- Deletes are scoped to the exact slugs/keys V13 seeded, so any content an
-- admin added through the CMS is left untouched. Reusable reference data
-- (amenities, room types, the brand-neutral FAQs) is kept as-is.
--
-- FK lookups use natural keys (iso2, slug, email) via subqueries, matching the
-- style of V13.

-- =====================================================================
-- 1. Remove the generic V13 demo content (dependency-safe order)
-- =====================================================================

-- Testimonials first: they reference packages/countries (ON DELETE SET NULL),
-- but we want the demo rows gone entirely, not just detached.
DELETE FROM testimonials
WHERE customer_name IN ('Sarah Mitchell', 'James Anderson', 'Priya Sharma',
                        'Marco Rossi', 'Emily Chen', 'David Thompson')
   OR package_id IN (SELECT id FROM tour_packages WHERE slug IN (
        'bangkok-city-explorer', 'phuket-island-paradise', 'roman-holiday-classic',
        'venice-romance-getaway', 'paris-city-of-lights', 'dubai-luxury-escape',
        'bali-tropical-retreat', 'yogyakarta-cultural-trail'));

DELETE FROM blog_posts WHERE slug IN (
    '10-hidden-gems-bangkok-beyond-grand-palace',
    'foodies-guide-rome-trastevere',
    'packing-smart-desert-safari',
    'bali-rice-terraces-sunrise',
    'first-timers-guide-venice-vaporetto',
    'paris-on-a-budget-free-things-to-do');

DELETE FROM banners WHERE title IN (
    'Discover the World with TourPackage',
    'Escape to Paradise',
    'Luxury, Redefined',
    'Your Next Adventure Awaits');

-- Packages cascade to their itinerary, images, includes and excludes.
DELETE FROM tour_packages WHERE slug IN (
    'bangkok-city-explorer', 'phuket-island-paradise', 'roman-holiday-classic',
    'venice-romance-getaway', 'paris-city-of-lights', 'dubai-luxury-escape',
    'bali-tropical-retreat', 'yogyakarta-cultural-trail');

-- Hotels cascade to their images, amenity links and rooms.
DELETE FROM hotels WHERE slug IN (
    'riverside-grand-bangkok', 'andaman-beach-resort', 'colosseo-boutique-hotel',
    'canal-view-palazzo', 'le-jardin-parisien', 'burj-oasis-hotel',
    'bali-serenity-villas', 'yogyakarta-heritage-inn');

DELETE FROM cities WHERE slug IN (
    'bangkok', 'phuket', 'rome', 'venice', 'paris', 'dubai', 'bali', 'yogyakarta');

DELETE FROM countries WHERE iso2 IN ('TH', 'IT', 'FR', 'AE', 'ID');

-- =====================================================================
-- 2. Geography — India, plus a handful of source countries for testimonials
-- =====================================================================
INSERT INTO countries (name, iso2, iso3, phone_code, currency_code, flag_emoji) VALUES
    ('India', 'IN', 'IND', '+91', 'INR', '🇮🇳'),
    ('United States', 'US', 'USA', '+1', 'USD', '🇺🇸'),
    ('United Kingdom', 'GB', 'GBR', '+44', 'GBP', '🇬🇧'),
    ('Australia', 'AU', 'AUS', '+61', 'AUD', '🇦🇺'),
    ('Germany', 'DE', 'DEU', '+49', 'EUR', '🇩🇪')
ON CONFLICT (iso2) DO NOTHING;

-- Ladakh region "destinations". state_province anchors them all to Ladakh.
INSERT INTO cities (country_id, name, slug, state_province, latitude, longitude, is_popular, image_url) VALUES
    ((SELECT id FROM countries WHERE iso2 = 'IN'), 'Leh', 'leh', 'Ladakh', 34.152588, 77.577049, TRUE,
     'https://images.unsplash.com/photo-1668966780008-b7dae98f61f3?w=1200&h=900&fit=crop&auto=format&q=80'),
    ((SELECT id FROM countries WHERE iso2 = 'IN'), 'Nubra Valley', 'nubra-valley', 'Ladakh', 34.550000, 77.550000, TRUE,
     'https://images.unsplash.com/photo-1686993999944-f7fd395cdcf7?w=1200&h=900&fit=crop&auto=format&q=80'),
    ((SELECT id FROM countries WHERE iso2 = 'IN'), 'Pangong Lake', 'pangong-lake', 'Ladakh', 33.750000, 78.660000, TRUE,
     'https://images.unsplash.com/photo-1660303954454-cc270a0d48c4?w=1200&h=900&fit=crop&auto=format&q=80'),
    ((SELECT id FROM countries WHERE iso2 = 'IN'), 'Zanskar Valley', 'zanskar-valley', 'Ladakh', 33.470000, 76.890000, TRUE,
     'https://images.unsplash.com/photo-1735909600958-f442498875f9?w=1200&h=900&fit=crop&auto=format&q=80'),
    ((SELECT id FROM countries WHERE iso2 = 'IN'), 'Kargil', 'kargil', 'Ladakh', 34.553900, 76.134900, FALSE,
     'https://images.unsplash.com/photo-1583602415713-a0319a0188c0?w=1200&h=900&fit=crop&auto=format&q=80'),
    ((SELECT id FROM countries WHERE iso2 = 'IN'), 'Tso Moriri', 'tso-moriri', 'Ladakh', 32.900000, 78.300000, FALSE,
     'https://images.unsplash.com/photo-1757007813494-f9e7b880d2c6?w=1200&h=900&fit=crop&auto=format&q=80'),
    ((SELECT id FROM countries WHERE iso2 = 'IN'), 'Sham Valley', 'sham-valley', 'Ladakh', 34.220000, 77.160000, FALSE,
     'https://images.unsplash.com/photo-1760835251791-1fda687de791?w=1200&h=900&fit=crop&auto=format&q=80'),
    ((SELECT id FROM countries WHERE iso2 = 'IN'), 'Turtuk', 'turtuk', 'Ladakh', 34.847000, 76.828000, FALSE,
     'https://images.unsplash.com/photo-1782317341310-335b55dc6537?w=1200&h=900&fit=crop&auto=format&q=80');

-- =====================================================================
-- 3. Hotels (+ one cover image each)
-- =====================================================================
INSERT INTO hotels (name, slug, short_description, description, star_rating, city_id, address_line1,
                    base_price, currency_code, rating_average, rating_count, is_featured, status, created_by) VALUES
    ('The Grand Dragon Ladakh', 'the-grand-dragon-ladakh',
     'Leh''s landmark stay, walking distance from the old town.',
     'A warm, high-comfort base in Leh with mountain-facing rooms, oxygen-rich lounges, and a kitchen that leans into Ladakhi and North Indian cooking.',
     5, (SELECT id FROM cities WHERE slug = 'leh'), 'Old Road, Sheynam, Leh',
     120.00, 'USD', 4.70, 512, TRUE, 'PUBLISHED', (SELECT id FROM admins WHERE email = 'admin@tourpackage.com')),
    ('Ladakh Sarai Resort', 'ladakh-sarai-resort',
     'Cottage-style rooms on the quiet edge of Leh.',
     'Stone-and-timber cottages set among poplar groves, built for slow acclimatisation days with long views of the Stok range.',
     4, (SELECT id FROM cities WHERE slug = 'leh'), 'Saboo Road, Leh',
     85.00, 'USD', 4.50, 288, FALSE, 'PUBLISHED', (SELECT id FROM admins WHERE email = 'admin@tourpackage.com')),
    ('Nubra Ecolodge', 'nubra-ecolodge',
     'Garden cottages beside the Hunder dunes.',
     'A low-impact lodge in Hunder where the cold desert meets green orchards — a short walk from the Bactrian camel dunes.',
     3, (SELECT id FROM cities WHERE slug = 'nubra-valley'), 'Hunder Village, Nubra',
     70.00, 'USD', 4.40, 176, FALSE, 'PUBLISHED', (SELECT id FROM admins WHERE email = 'admin@tourpackage.com')),
    ('Pangong Camp Retreat', 'pangong-camp-retreat',
     'Deluxe tents on the shore of Pangong Tso.',
     'Insulated deluxe tents a stone''s throw from the water, positioned for sunrise over the lake and some of the darkest night skies in India.',
     3, (SELECT id FROM cities WHERE slug = 'pangong-lake'), 'Spangmik, Pangong Tso',
     90.00, 'USD', 4.60, 203, TRUE, 'PUBLISHED', (SELECT id FROM admins WHERE email = 'admin@tourpackage.com')),
    ('Zanskar Highland Resort', 'zanskar-highland-resort',
     'A quiet highland base in Padum.',
     'Simple, well-heated rooms in Padum — the natural staging point for treks and river trips deeper into the Zanskar valley.',
     4, (SELECT id FROM cities WHERE slug = 'zanskar-valley'), 'Main Bazaar, Padum',
     95.00, 'USD', 4.50, 97, FALSE, 'PUBLISHED', (SELECT id FROM admins WHERE email = 'admin@tourpackage.com')),
    ('Kargil Continental', 'kargil-continental',
     'A comfortable halt on the Srinagar–Leh road.',
     'A dependable overnight stay in Kargil with river-facing rooms, handy for breaking the long drive between Sonamarg and Leh.',
     3, (SELECT id FROM cities WHERE slug = 'kargil'), 'Baroo, Kargil',
     60.00, 'USD', 4.20, 64, FALSE, 'PUBLISHED', (SELECT id FROM admins WHERE email = 'admin@tourpackage.com'));

INSERT INTO hotel_images (hotel_id, url, alt_text, is_cover) VALUES
    ((SELECT id FROM hotels WHERE slug = 'the-grand-dragon-ladakh'),
     'https://images.unsplash.com/photo-1668966780008-b7dae98f61f3?w=1200&h=800&fit=crop&auto=format&q=80', 'The Grand Dragon Ladakh against the mountains', TRUE),
    ((SELECT id FROM hotels WHERE slug = 'ladakh-sarai-resort'),
     'https://images.unsplash.com/photo-1760835251791-1fda687de791?w=1200&h=800&fit=crop&auto=format&q=80', 'Ladakh Sarai Resort in the hills', TRUE),
    ((SELECT id FROM hotels WHERE slug = 'nubra-ecolodge'),
     'https://images.unsplash.com/photo-1686993999944-f7fd395cdcf7?w=1200&h=800&fit=crop&auto=format&q=80', 'Nubra Ecolodge near the desert road', TRUE),
    ((SELECT id FROM hotels WHERE slug = 'pangong-camp-retreat'),
     'https://images.unsplash.com/photo-1757007813494-f9e7b880d2c6?w=1200&h=800&fit=crop&auto=format&q=80', 'Pangong Camp Retreat by the lake', TRUE),
    ((SELECT id FROM hotels WHERE slug = 'zanskar-highland-resort'),
     'https://images.unsplash.com/photo-1735909600958-f442498875f9?w=1200&h=800&fit=crop&auto=format&q=80', 'Zanskar Highland Resort among the peaks', TRUE),
    ((SELECT id FROM hotels WHERE slug = 'kargil-continental'),
     'https://images.unsplash.com/photo-1583602415713-a0319a0188c0?w=1200&h=800&fit=crop&auto=format&q=80', 'Kargil Continental by the river', TRUE);

-- =====================================================================
-- 4. Tour packages (+ cover image each). Difficulty is set per package;
--    five carry a discount_price, which is what "Special Offers" filters on.
-- =====================================================================
INSERT INTO tour_packages (title, slug, summary, description, country_id, city_id, duration_days,
                           duration_nights, price, discount_price, difficulty_level, rating_average,
                           rating_count, is_featured, status, created_by) VALUES
    ('Leh Ladakh Complete Circuit', 'leh-ladakh-complete-circuit',
     'The full loop — Leh, Nubra, Pangong, and the high passes in between.',
     'Seven days across the classic Ladakh circuit: acclimatise in Leh, cross Khardung La into the Nubra Valley, then swing south to Pangong Tso before returning over Chang La.',
     (SELECT id FROM countries WHERE iso2 = 'IN'), (SELECT id FROM cities WHERE slug = 'leh'),
     7, 6, 899.00, NULL, 'CHALLENGING', 4.80, 342, TRUE, 'PUBLISHED', (SELECT id FROM admins WHERE email = 'admin@tourpackage.com')),
    ('Nubra Valley & Pangong Explorer', 'nubra-valley-pangong-explorer',
     'Cold-desert dunes and a lake that changes colour with the sky.',
     'Six days linking the Hunder sand dunes and Diskit monastery of the Nubra Valley with the turquoise expanse of Pangong Tso.',
     (SELECT id FROM countries WHERE iso2 = 'IN'), (SELECT id FROM cities WHERE slug = 'nubra-valley'),
     6, 5, 749.00, 649.00, 'MODERATE', 4.70, 288, FALSE, 'PUBLISHED', (SELECT id FROM admins WHERE email = 'admin@tourpackage.com')),
    ('Leh Sightseeing Getaway', 'leh-sightseeing-getaway',
     'A short break around Leh''s monasteries and old town.',
     'Four easy days built for a first taste of Ladakh — Shanti Stupa, Leh Palace, Thiksey and Hemis, with time set aside to acclimatise.',
     (SELECT id FROM countries WHERE iso2 = 'IN'), (SELECT id FROM cities WHERE slug = 'leh'),
     4, 3, 349.00, 299.00, 'EASY', 4.50, 176, FALSE, 'PUBLISHED', (SELECT id FROM admins WHERE email = 'admin@tourpackage.com')),
    ('Zanskar Valley Adventure', 'zanskar-valley-adventure',
     'Deep into one of the Himalaya''s most remote valleys.',
     'Eight days over the Pensi La into Zanskar — Padum, cliffside gompas at Karsha and Phugtal, and the milky rush of the Zanskar river.',
     (SELECT id FROM countries WHERE iso2 = 'IN'), (SELECT id FROM cities WHERE slug = 'zanskar-valley'),
     8, 7, 1199.00, NULL, 'CHALLENGING', 4.90, 154, TRUE, 'PUBLISHED', (SELECT id FROM admins WHERE email = 'admin@tourpackage.com')),
    ('Pangong Lake Camping Escape', 'pangong-lake-camping-escape',
     'Two nights under some of India''s darkest skies.',
     'Five days easing up from Leh to a lakeside camp at Pangong Tso, timed for sunrise on the water and a full night of stargazing.',
     (SELECT id FROM countries WHERE iso2 = 'IN'), (SELECT id FROM cities WHERE slug = 'pangong-lake'),
     5, 4, 549.00, 449.00, 'MODERATE', 4.60, 263, FALSE, 'PUBLISHED', (SELECT id FROM admins WHERE email = 'admin@tourpackage.com')),
    ('Tso Moriri & Changthang Expedition', 'tso-moriri-changthang-expedition',
     'The high plateau, its nomads, and a lake at 4,500 metres.',
     'Seven days across the Changthang plateau to Tso Moriri and Tso Kar, through Korzok village and the summer camps of the Changpa herders.',
     (SELECT id FROM countries WHERE iso2 = 'IN'), (SELECT id FROM cities WHERE slug = 'tso-moriri'),
     7, 6, 999.00, NULL, 'CHALLENGING', 4.70, 121, FALSE, 'PUBLISHED', (SELECT id FROM admins WHERE email = 'admin@tourpackage.com')),
    ('Sham Valley Cultural Trail', 'sham-valley-cultural-trail',
     'The gentle "apricot valley" west of Leh.',
     'Four unhurried days through the Sham Valley — Alchi''s ancient murals, Likir and Basgo, and the confluence of the Indus and Zanskar rivers.',
     (SELECT id FROM countries WHERE iso2 = 'IN'), (SELECT id FROM cities WHERE slug = 'sham-valley'),
     4, 3, 379.00, 329.00, 'EASY', 4.40, 98, FALSE, 'PUBLISHED', (SELECT id FROM admins WHERE email = 'admin@tourpackage.com')),
    ('Kargil & Drass Heritage Tour', 'kargil-drass-heritage-tour',
     'The Srinagar–Leh road, at its own pace.',
     'Five days along the historic Kargil route — the Drass war memorial, the Mulbekh Maitreya, and the mountain drive into Ladakh.',
     (SELECT id FROM countries WHERE iso2 = 'IN'), (SELECT id FROM cities WHERE slug = 'kargil'),
     5, 4, 629.00, NULL, 'MODERATE', 4.50, 87, FALSE, 'PUBLISHED', (SELECT id FROM admins WHERE email = 'admin@tourpackage.com')),
    ('Turtuk & Nubra Hidden Villages', 'turtuk-nubra-hidden-villages',
     'To the last villages before the Line of Control.',
     'Six days beyond Nubra to Turtuk and the Balti villages of the Shyok — apricot orchards, stone hamlets, and a culture found nowhere else in India.',
     (SELECT id FROM countries WHERE iso2 = 'IN'), (SELECT id FROM cities WHERE slug = 'turtuk'),
     6, 5, 799.00, 699.00, 'MODERATE', 4.80, 112, TRUE, 'PUBLISHED', (SELECT id FROM admins WHERE email = 'admin@tourpackage.com'));

INSERT INTO package_images (package_id, url, alt_text, is_cover) VALUES
    ((SELECT id FROM tour_packages WHERE slug = 'leh-ladakh-complete-circuit'),
     'https://images.unsplash.com/photo-1668966780008-b7dae98f61f3?w=1200&h=800&fit=crop&auto=format&q=80', 'Leh town below the mountains', TRUE),
    ((SELECT id FROM tour_packages WHERE slug = 'nubra-valley-pangong-explorer'),
     'https://images.unsplash.com/photo-1660303954454-cc270a0d48c4?w=1200&h=800&fit=crop&auto=format&q=80', 'Pangong Tso and the ranges beyond', TRUE),
    ((SELECT id FROM tour_packages WHERE slug = 'leh-sightseeing-getaway'),
     'https://images.unsplash.com/photo-1760835251791-1fda687de791?w=1200&h=800&fit=crop&auto=format&q=80', 'A Ladakhi monastery in the hills', TRUE),
    ((SELECT id FROM tour_packages WHERE slug = 'zanskar-valley-adventure'),
     'https://images.unsplash.com/photo-1735909600958-f442498875f9?w=1200&h=800&fit=crop&auto=format&q=80', 'A village deep in the Zanskar ranges', TRUE),
    ((SELECT id FROM tour_packages WHERE slug = 'pangong-lake-camping-escape'),
     'https://images.unsplash.com/photo-1757007813494-f9e7b880d2c6?w=1200&h=800&fit=crop&auto=format&q=80', 'The shore of Pangong Tso', TRUE),
    ((SELECT id FROM tour_packages WHERE slug = 'tso-moriri-changthang-expedition'),
     'https://images.unsplash.com/photo-1583602415713-a0319a0188c0?w=1200&h=800&fit=crop&auto=format&q=80', 'A high-plateau lake ringed by mountains', TRUE),
    ((SELECT id FROM tour_packages WHERE slug = 'sham-valley-cultural-trail'),
     'https://images.unsplash.com/photo-1782317341310-335b55dc6537?w=1200&h=800&fit=crop&auto=format&q=80', 'A monastery on a green Ladakhi slope', TRUE),
    ((SELECT id FROM tour_packages WHERE slug = 'kargil-drass-heritage-tour'),
     'https://images.unsplash.com/photo-1583602415713-a0319a0188c0?w=1200&h=800&fit=crop&auto=format&q=80', 'Mountains along the Kargil road', TRUE),
    ((SELECT id FROM tour_packages WHERE slug = 'turtuk-nubra-hidden-villages'),
     'https://images.unsplash.com/photo-1686993999944-f7fd395cdcf7?w=1200&h=800&fit=crop&auto=format&q=80', 'The desert road into the Nubra Valley', TRUE);

-- ---------------------------------------------------------------------
-- Itinerary, includes and excludes for the new packages. V15 only ran
-- against the (now-removed) demo packages, so these are generated the same
-- generic way here, scoped to packages that don't already have an itinerary.
-- ---------------------------------------------------------------------
INSERT INTO package_itinerary (package_id, day_number, title, description, city_id, meals, accommodation)
SELECT
    tp.id,
    d.day_number,
    CASE
        WHEN d.day_number = 1 THEN 'Arrival in ' || c.name
        WHEN d.day_number = tp.duration_days THEN 'Departure from ' || c.name
        ELSE 'Exploring ' || c.name || ' — Day ' || d.day_number
    END,
    CASE
        WHEN d.day_number = 1 THEN
            'Arrive in ' || c.name || ', meet your guide, and transfer to the hotel. The rest of the day is kept free to rest and acclimatise to the altitude.'
        WHEN d.day_number = tp.duration_days THEN
            'Breakfast, then transfer for your onward journey out of ' || c.name || '.'
        ELSE
            'A full day of guided sightseeing around ' || c.name || ', with time set aside for lunch and independent exploration.'
    END,
    tp.city_id,
    CASE
        WHEN d.day_number = 1 THEN 'Dinner'
        WHEN d.day_number = tp.duration_days THEN 'Breakfast'
        ELSE 'Breakfast, Lunch'
    END,
    CASE WHEN d.day_number = tp.duration_days THEN NULL ELSE c.name || ' Stay' END
FROM tour_packages tp
JOIN cities c ON c.id = tp.city_id
CROSS JOIN LATERAL generate_series(1, tp.duration_days) AS d(day_number)
WHERE NOT EXISTS (SELECT 1 FROM package_itinerary pit WHERE pit.package_id = tp.id);

INSERT INTO package_includes (package_id, description, icon, display_order)
SELECT tp.id, v.description, v.icon, v.display_order
FROM tour_packages tp
CROSS JOIN (VALUES
    ('Accommodation for the full duration of the tour', 'bed-double', 1),
    ('Daily breakfast, and dinner where noted', 'utensils', 2),
    ('All transport in a private vehicle with driver', 'car', 3),
    ('Professional English-speaking local guide', 'user-round', 4),
    ('Inner Line Permits for restricted areas', 'ticket', 5),
    ('All applicable taxes and service charges', 'receipt', 6)
) AS v(description, icon, display_order)
WHERE NOT EXISTS (SELECT 1 FROM package_includes pin WHERE pin.package_id = tp.id);

INSERT INTO package_includes (package_id, description, icon, display_order)
SELECT tp.id, 'Oxygen support and a first-aid kit for high-altitude days', 'heart-pulse', 7
FROM tour_packages tp
WHERE tp.difficulty_level IN ('MODERATE', 'CHALLENGING', 'EXTREME')
  AND NOT EXISTS (
      SELECT 1 FROM package_includes pin
      WHERE pin.package_id = tp.id AND pin.display_order = 7);

INSERT INTO package_excludes (package_id, description, icon, display_order)
SELECT tp.id, v.description, v.icon, v.display_order
FROM tour_packages tp
CROSS JOIN (VALUES
    ('Flights to and from Leh', 'plane', 1),
    ('Travel insurance', 'shield', 2),
    ('Lunches and dinners not listed in the itinerary', 'utensils-crossed', 3),
    ('Personal expenses, camera fees, and tips', 'wallet', 4),
    ('Anything not explicitly listed under inclusions', 'circle-minus', 5)
) AS v(description, icon, display_order)
WHERE NOT EXISTS (SELECT 1 FROM package_excludes pex WHERE pex.package_id = tp.id);

-- =====================================================================
-- 5. Hotel amenities & rooms for the new hotels (V14 only ran against the
--    demo hotels). Reuses the amenities/room_types reference data V14 seeded.
-- =====================================================================
INSERT INTO hotel_amenities (hotel_id, amenity_id)
SELECT h.id, a.id FROM hotels h
JOIN amenities a ON a.slug IN ('free-wifi', 'air-conditioning', '24-hour-front-desk', 'free-parking', 'restaurant')
WHERE NOT EXISTS (SELECT 1 FROM hotel_amenities ha WHERE ha.hotel_id = h.id);

INSERT INTO hotel_amenities (hotel_id, amenity_id)
SELECT h.id, a.id FROM hotels h
JOIN amenities a ON a.slug IN ('airport-shuttle', 'bar-lounge')
WHERE h.star_rating >= 4
  AND NOT EXISTS (SELECT 1 FROM hotel_amenities ha WHERE ha.hotel_id = h.id AND ha.amenity_id = a.id);

INSERT INTO hotel_amenities (hotel_id, amenity_id)
SELECT h.id, a.id FROM hotels h
JOIN amenities a ON a.slug IN ('spa-wellness', 'fitness-center')
WHERE h.star_rating = 5
  AND NOT EXISTS (SELECT 1 FROM hotel_amenities ha WHERE ha.hotel_id = h.id AND ha.amenity_id = a.id);

INSERT INTO hotel_rooms (
    hotel_id, room_type_id, name, description, max_adults, max_children,
    bed_count, bed_type, size_sqm, price_per_night, currency_code, total_rooms, is_active
)
SELECT
    h.id,
    rt.id,
    h.name || ' - ' || rt.name,
    rt.description,
    rt.max_occupancy,
    CASE rt.slug WHEN 'executive-suite' THEN 2 ELSE 1 END,
    CASE rt.slug WHEN 'executive-suite' THEN 2 ELSE 1 END,
    'King',
    CASE rt.slug
        WHEN 'standard-room' THEN 26
        WHEN 'deluxe-room' THEN 34
        WHEN 'executive-suite' THEN 48
    END,
    ROUND(h.base_price * CASE rt.slug
        WHEN 'standard-room' THEN 1.0
        WHEN 'deluxe-room' THEN 1.35
        WHEN 'executive-suite' THEN 2.1
    END, 2),
    h.currency_code,
    CASE rt.slug
        WHEN 'standard-room' THEN 12
        WHEN 'deluxe-room' THEN 8
        WHEN 'executive-suite' THEN 3
    END,
    TRUE
FROM hotels h
CROSS JOIN room_types rt
WHERE NOT EXISTS (SELECT 1 FROM hotel_rooms hr WHERE hr.hotel_id = h.id);

-- =====================================================================
-- 6. Testimonials
-- =====================================================================
INSERT INTO testimonials (package_id, customer_name, customer_avatar_url, customer_country_id, rating, message, is_featured, is_active) VALUES
    ((SELECT id FROM tour_packages WHERE slug = 'leh-ladakh-complete-circuit'), 'Aditya Menon',
     'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=200&h=200&fit=crop&auto=format&q=80',
     (SELECT id FROM countries WHERE iso2 = 'IN'), 5,
     'The whole circuit was paced perfectly — two days in Leh before we went high meant nobody in our group struggled with the altitude. Khardung La and Pangong on the same trip is hard to beat.',
     TRUE, TRUE),
    ((SELECT id FROM tour_packages WHERE slug = 'pangong-lake-camping-escape'), 'Sarah Whitfield',
     'https://images.unsplash.com/photo-1487412720507-e7ab37603c6f?w=200&h=200&fit=crop&auto=format&q=80',
     (SELECT id FROM countries WHERE iso2 = 'GB'), 5,
     'Waking up to sunrise over Pangong from a warm tent is something I''ll remember for years. The camp team looked after every detail, right down to hot water bottles at night.',
     TRUE, TRUE),
    ((SELECT id FROM tour_packages WHERE slug = 'turtuk-nubra-hidden-villages'), 'Priya Nair',
     'https://images.unsplash.com/photo-1524250502761-1ac6f2e30d43?w=200&h=200&fit=crop&auto=format&q=80',
     (SELECT id FROM countries WHERE iso2 = 'IN'), 5,
     'Turtuk felt like stepping into another world. Our guide knew the families there, and the apricots straight off the tree were unforgettable. So glad we went beyond the usual route.',
     TRUE, TRUE),
    ((SELECT id FROM tour_packages WHERE slug = 'zanskar-valley-adventure'), 'Daniel Fischer',
     'https://images.unsplash.com/photo-1560250097-0b93528c311a?w=200&h=200&fit=crop&auto=format&q=80',
     (SELECT id FROM countries WHERE iso2 = 'DE'), 5,
     'Zanskar is genuinely remote and the team was completely on top of the logistics — permits, fuel, backup plans for the passes. Phugtal monastery alone was worth the long drive.',
     TRUE, TRUE),
    ((SELECT id FROM tour_packages WHERE slug = 'nubra-valley-pangong-explorer'), 'Emma Sullivan',
     'https://images.unsplash.com/photo-1531123897727-8f129e1688ce?w=200&h=200&fit=crop&auto=format&q=80',
     (SELECT id FROM countries WHERE iso2 = 'AU'), 4,
     'A brilliant six days. The Hunder dunes at sunset and then Pangong two days later — the contrast is wild. Only wish we''d had one more night in Nubra.',
     TRUE, TRUE),
    ((SELECT id FROM tour_packages WHERE slug = 'leh-sightseeing-getaway'), 'Rohan Kapoor',
     'https://images.unsplash.com/photo-1531891437562-4301cf35b7e4?w=200&h=200&fit=crop&auto=format&q=80',
     (SELECT id FROM countries WHERE iso2 = 'IN'), 5,
     'Perfect short trip for a first visit. Thiksey at morning prayers was the highlight, and they built in enough rest that we actually enjoyed the altitude instead of fighting it.',
     TRUE, TRUE);

-- =====================================================================
-- 7. Banners (hero slider)
-- =====================================================================
INSERT INTO banners (title, subtitle, image_url, link_url, button_label, display_order, is_active) VALUES
    ('Discover Leh Ladakh', 'Journeys across the roof of the world — high passes, still lakes, and ancient monasteries.',
     'https://images.unsplash.com/photo-1660303954454-cc270a0d48c4?w=1920&h=1080&fit=crop&auto=format&q=80',
     '/packages', 'Explore Packages', 1, TRUE),
    ('Ride the Highest Roads on Earth', 'Over Khardung La into the Nubra Valley and the Shyok beyond.',
     'https://images.unsplash.com/photo-1686993999944-f7fd395cdcf7?w=1920&h=1080&fit=crop&auto=format&q=80',
     '/packages', 'View Tours', 2, TRUE),
    ('Camp Beside Pangong Tso', 'Wake to a lake that changes colour with the sky, under some of India''s darkest skies.',
     'https://images.unsplash.com/photo-1757007813494-f9e7b880d2c6?w=1920&h=1080&fit=crop&auto=format&q=80',
     '/offers', 'See Offers', 3, TRUE),
    ('Where the Himalayas Meet the Sky', 'Monasteries, prayer flags, and starlit desert nights across Ladakh.',
     'https://images.unsplash.com/photo-1760835251791-1fda687de791?w=1920&h=1080&fit=crop&auto=format&q=80',
     '/destinations', 'Browse Destinations', 4, TRUE);

-- =====================================================================
-- 8. Blog posts
-- =====================================================================
INSERT INTO blog_posts (title, slug, excerpt, content, cover_image_url, category, author_id, status, published_at, read_time_minutes) VALUES
    ('Acclimatising in Leh: Your First 48 Hours at Altitude', 'acclimatising-in-leh-first-48-hours',
     'Leh sits at 3,500 metres, and the two days after you land matter more than any sight on your itinerary. Here''s how to spend them.',
     'A practical guide to arriving in Leh: why to keep the first day flat and slow, how to read the early signs of altitude sickness, staying hydrated, and the gentle short walks that help your body adjust before you head for the high passes.',
     'https://images.unsplash.com/photo-1668966780008-b7dae98f61f3?w=1200&h=800&fit=crop&auto=format&q=80',
     'Travel Tips', (SELECT id FROM admins WHERE email = 'admin@tourpackage.com'), 'PUBLISHED', now() - interval '3 days', 6),
    ('Pangong Tso: Why the Lake Changes Colour Through the Day', 'pangong-tso-changing-colours',
     'From steel grey at dawn to deep turquoise by noon — the science and the best hours to see Pangong at its most vivid.',
     'A look at what gives Pangong Tso its shifting palette, the times of day worth planning around, where to stand for the best light, and why an overnight at the lake beats a day trip.',
     'https://images.unsplash.com/photo-1660303954454-cc270a0d48c4?w=1200&h=800&fit=crop&auto=format&q=80',
     'Destination Guide', (SELECT id FROM admins WHERE email = 'admin@tourpackage.com'), 'PUBLISHED', now() - interval '8 days', 5),
    ('A Rider''s Guide to Khardung La and the Nubra Valley', 'riders-guide-khardung-la-nubra',
     'What to expect on one of the world''s highest motorable roads — and the descent into the cold desert on the far side.',
     'Everything a first-time rider should know about the climb to Khardung La and the drop into Nubra: permits, fuel stops, road conditions by season, and the villages worth pausing for on the way to Hunder.',
     'https://images.unsplash.com/photo-1686993999944-f7fd395cdcf7?w=1200&h=800&fit=crop&auto=format&q=80',
     'Travel Tips', (SELECT id FROM admins WHERE email = 'admin@tourpackage.com'), 'PUBLISHED', now() - interval '12 days', 7),
    ('Inside Ladakh''s Monasteries: Thiksey, Hemis, and Diskit', 'ladakh-monasteries-thiksey-hemis-diskit',
     'A short introduction to three of Ladakh''s great gompas — when to visit, what to look for, and how to be a respectful guest.',
     'A walk through Thiksey, Hemis, and Diskit: their history, the festivals worth timing a trip around, the etiquette of visiting a working monastery, and why morning prayers are the moment to aim for.',
     'https://images.unsplash.com/photo-1760835251791-1fda687de791?w=1200&h=800&fit=crop&auto=format&q=80',
     'Food & Culture', (SELECT id FROM admins WHERE email = 'admin@tourpackage.com'), 'PUBLISHED', now() - interval '17 days', 6),
    ('Packing for Ladakh: What to Bring for Cold Desert Days', 'packing-for-ladakh-cold-desert',
     'Thirty-degree days and near-freezing nights in the same 24 hours — here''s the layering system our guides swear by.',
     'A season-by-season packing list for Ladakh: layering for big temperature swings, sun and wind protection at altitude, what to carry for lakeside camps, and the small things travellers most often forget.',
     'https://images.unsplash.com/photo-1735909600958-f442498875f9?w=1200&h=800&fit=crop&auto=format&q=80',
     'Travel Tips', (SELECT id FROM admins WHERE email = 'admin@tourpackage.com'), 'PUBLISHED', now() - interval '21 days', 5),
    ('Zanskar: The Valley That Winter Cuts Off From the World', 'zanskar-valley-winter-chadar',
     'For half the year the only way in is a frozen river. A look at Zanskar''s remoteness — and the best window to visit.',
     'An introduction to the Zanskar valley: how the Pensi La closes with the snow, the famous frozen-river Chadar route, the cliffside monastery of Phugtal, and why summer is the season to plan an overland trip.',
     'https://images.unsplash.com/photo-1782317341310-335b55dc6537?w=1200&h=800&fit=crop&auto=format&q=80',
     'Destination Guide', (SELECT id FROM admins WHERE email = 'admin@tourpackage.com'), 'PUBLISHED', now() - interval '26 days', 6);

-- =====================================================================
-- 9. Public settings — site identity & contact. Placeholder brand details;
--    replace with your own via the admin settings screen.
-- =====================================================================
INSERT INTO settings (key, value, value_type, group_name, is_public) VALUES
    ('site_name', 'Leh Ladakh Tours', 'STRING', 'GENERAL', TRUE),
    ('site_tagline', 'Journeys across the roof of the world.', 'STRING', 'GENERAL', TRUE),
    ('contact_email', 'hello@lehladakhtours.example', 'STRING', 'CONTACT', TRUE),
    ('contact_phone', '+91 98765 43210', 'STRING', 'CONTACT', TRUE),
    ('contact_address', 'Main Bazaar Road, Leh, Ladakh 194101, India', 'STRING', 'CONTACT', TRUE),
    ('social_facebook', 'https://facebook.com/lehladakhtours', 'STRING', 'SOCIAL', TRUE),
    ('social_instagram', 'https://instagram.com/lehladakhtours', 'STRING', 'SOCIAL', TRUE),
    ('social_twitter', 'https://twitter.com/lehladakhtours', 'STRING', 'SOCIAL', TRUE),
    ('social_youtube', 'https://youtube.com/@lehladakhtours', 'STRING', 'SOCIAL', TRUE),
    ('business_hours', 'Mon–Sat, 9:00 AM – 7:00 PM (IST)', 'STRING', 'CONTACT', TRUE)
ON CONFLICT (key) DO UPDATE
    SET value = EXCLUDED.value,
        value_type = EXCLUDED.value_type,
        group_name = EXCLUDED.group_name,
        is_public = EXCLUDED.is_public;
