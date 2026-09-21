-- Replaces the placeholder hotels seeded in V21 with the operator's real
-- properties, split into Premium and Budget tiers.
--
-- There is no dedicated "tier" column on hotels, so the tier is encoded two
-- ways the app already understands: star_rating (Premium = 5, Budget = 3) — so
-- the existing star-rating filter doubles as a tier filter — and the tier is
-- stated at the front of short_description. The web hotel card also shows a
-- "Premium"/"Budget" badge derived from the star rating.
--
-- No rates yet: base_price stays 0 and the web app shows "Rate on request"
-- instead of a price. currency_code is INR so any rate added later renders as ₹.
-- Rooms are intentionally not seeded (they would otherwise appear as ₹0).
--
-- Images are the free-licence Unsplash photos used elsewhere in the seed, on
-- images.unsplash.com (the only host the web app allow-lists).

-- =====================================================================
-- 1. New cities for the hotel locations (stopovers, not headline
--    destinations, so is_popular stays false).
-- =====================================================================
INSERT INTO cities (country_id, name, slug, state_province, latitude, longitude, is_popular, image_url) VALUES
    ((SELECT id FROM countries WHERE iso2 = 'IN'), 'Manali', 'manali', 'Himachal Pradesh', 32.239600, 77.188700, FALSE,
     'https://images.unsplash.com/photo-1782317341310-335b55dc6537?w=1200&h=900&fit=crop&auto=format&q=80'),
    ((SELECT id FROM countries WHERE iso2 = 'IN'), 'Jispa', 'jispa', 'Himachal Pradesh', 32.616700, 77.250000, FALSE,
     'https://images.unsplash.com/photo-1735909600958-f442498875f9?w=1200&h=900&fit=crop&auto=format&q=80'),
    ((SELECT id FROM countries WHERE iso2 = 'IN'), 'Sarchu', 'sarchu', 'Himachal Pradesh', 32.916700, 77.500000, FALSE,
     'https://images.unsplash.com/photo-1583602415713-a0319a0188c0?w=1200&h=900&fit=crop&auto=format&q=80'),
    ((SELECT id FROM countries WHERE iso2 = 'IN'), 'Hanle', 'hanle', 'Ladakh', 32.779200, 79.018600, FALSE,
     'https://images.unsplash.com/photo-1656937693729-8dc58b9d6c74?w=1200&h=900&fit=crop&auto=format&q=80');

-- =====================================================================
-- 2. Remove the V21 placeholder hotels (cascades images, amenity links, rooms).
-- =====================================================================
DELETE FROM hotels WHERE slug IN (
    'the-grand-dragon-ladakh', 'ladakh-sarai-resort', 'nubra-ecolodge',
    'pangong-camp-retreat', 'zanskar-highland-resort', 'kargil-continental');

-- =====================================================================
-- 3. Premium hotels (5-star tier, featured)
-- =====================================================================
INSERT INTO hotels (name, slug, short_description, description, star_rating, city_id, address_line1,
                    base_price, currency_code, is_featured, status, created_by) VALUES
    ('Solang Valley Resort', 'solang-valley-resort-manali',
     'Premium — mountain resort in the Solang Valley near Manali.',
     'A premium resort amid the pine slopes of the Solang Valley, a short drive from Manali — a comfortable first or last night on the Manali–Leh route.',
     5, (SELECT id FROM cities WHERE slug = 'manali'), 'Solang Valley, Manali',
     0.00, 'INR', TRUE, 'PUBLISHED', (SELECT id FROM admins WHERE email = 'admin@tourpackage.com')),
    ('Ayim Hotel, Saboo', 'ayim-hotel-saboo-leh',
     'Premium — quiet hotel in Saboo, on the edge of Leh.',
     'A premium stay in Saboo village on the outskirts of Leh, with mountain views and easy access to the old town and market.',
     5, (SELECT id FROM cities WHERE slug = 'leh'), 'Saboo, Leh',
     0.00, 'INR', TRUE, 'PUBLISHED', (SELECT id FROM admins WHERE email = 'admin@tourpackage.com')),
    ('Gurudi Retreat, Hunder', 'gurudi-retreat-hunder-nubra',
     'Premium — garden retreat by the Hunder dunes.',
     'A premium retreat in Hunder, set among orchards a short walk from the Nubra Valley''s white sand dunes.',
     5, (SELECT id FROM cities WHERE slug = 'nubra-valley'), 'Hunder, Nubra Valley',
     0.00, 'INR', TRUE, 'PUBLISHED', (SELECT id FROM admins WHERE email = 'admin@tourpackage.com')),
    ('The Grand Pangong', 'the-grand-pangong',
     'Premium — lakeside stay at Pangong Tso.',
     'A premium stay close to the shore of Pangong Tso, positioned for sunrise over the lake.',
     5, (SELECT id FROM cities WHERE slug = 'pangong-lake'), 'Spangmik, Pangong Tso',
     0.00, 'INR', TRUE, 'PUBLISHED', (SELECT id FROM admins WHERE email = 'admin@tourpackage.com')),
    ('Barley Cabin, Merak', 'barley-cabin-merak',
     'Premium — cabins in Merak village on Pangong Tso.',
     'Premium cabins in the quiet Merak village on the southern shore of Pangong Tso, away from the crowds.',
     5, (SELECT id FROM cities WHERE slug = 'pangong-lake'), 'Merak, Pangong Tso',
     0.00, 'INR', TRUE, 'PUBLISHED', (SELECT id FROM admins WHERE email = 'admin@tourpackage.com')),
    ('The Mor, Hanle', 'the-mor-hanle',
     'Premium — stay under Hanle''s dark skies.',
     'A premium stay in Hanle, home to India''s dark-sky reserve and one of the country''s finest stargazing spots.',
     5, (SELECT id FROM cities WHERE slug = 'hanle'), 'Hanle Village',
     0.00, 'INR', TRUE, 'PUBLISHED', (SELECT id FROM admins WHERE email = 'admin@tourpackage.com')),
    ('Wisdom House', 'wisdom-house-tso-moriri',
     'Premium — comfortable house by Tso Moriri.',
     'A premium house near the high-altitude Tso Moriri lake on the Changthang plateau.',
     5, (SELECT id FROM cities WHERE slug = 'tso-moriri'), 'Korzok, Tso Moriri',
     0.00, 'INR', TRUE, 'PUBLISHED', (SELECT id FROM admins WHERE email = 'admin@tourpackage.com'));

-- =====================================================================
-- 4. Budget hotels (3-star tier)
-- =====================================================================
INSERT INTO hotels (name, slug, short_description, description, star_rating, city_id, address_line1,
                    base_price, currency_code, is_featured, status, created_by) VALUES
    ('Ibex Hotel, Manali', 'ibex-hotel-manali',
     'Budget — dependable hotel in Manali.',
     'A budget-friendly hotel in Manali, a handy base at the start or end of the Manali–Leh road journey.',
     3, (SELECT id FROM cities WHERE slug = 'manali'), 'Mall Road, Manali',
     0.00, 'INR', FALSE, 'PUBLISHED', (SELECT id FROM admins WHERE email = 'admin@tourpackage.com')),
    ('Ibex Hotel, Jispa', 'ibex-hotel-jispa',
     'Budget — roadside hotel in Jispa.',
     'A simple budget hotel in Jispa on the Manali–Leh highway, a useful acclimatisation halt.',
     3, (SELECT id FROM cities WHERE slug = 'jispa'), 'Jispa, Lahaul',
     0.00, 'INR', FALSE, 'PUBLISHED', (SELECT id FROM admins WHERE email = 'admin@tourpackage.com')),
    ('Dorjey Camp, Sarchu', 'dorjey-camp-sarchu',
     'Budget — highway camp at Sarchu.',
     'A budget tented camp at Sarchu, the classic overnight stop on the Manali–Leh highway.',
     3, (SELECT id FROM cities WHERE slug = 'sarchu'), 'Sarchu, Manali–Leh Highway',
     0.00, 'INR', FALSE, 'PUBLISHED', (SELECT id FROM admins WHERE email = 'admin@tourpackage.com')),
    ('Tara Hotel, Leh', 'tara-hotel-leh',
     'Budget — central hotel in Leh.',
     'A budget hotel in Leh within easy reach of the main market and the old town.',
     3, (SELECT id FROM cities WHERE slug = 'leh'), 'Main Bazaar, Leh',
     0.00, 'INR', FALSE, 'PUBLISHED', (SELECT id FROM admins WHERE email = 'admin@tourpackage.com')),
    ('Summer Camp, Nubra', 'summer-camp-nubra',
     'Budget — tented camp in the Nubra Valley.',
     'A budget tented camp in the Nubra Valley, close to the Hunder dunes.',
     3, (SELECT id FROM cities WHERE slug = 'nubra-valley'), 'Hunder, Nubra Valley',
     0.00, 'INR', FALSE, 'PUBLISHED', (SELECT id FROM admins WHERE email = 'admin@tourpackage.com')),
    ('Hidden Heaven, Merak', 'hidden-heaven-merak',
     'Budget — homely stay in Merak on Pangong Tso.',
     'A budget stay in Merak village on the quieter southern shore of Pangong Tso.',
     3, (SELECT id FROM cities WHERE slug = 'pangong-lake'), 'Merak, Pangong Tso',
     0.00, 'INR', FALSE, 'PUBLISHED', (SELECT id FROM admins WHERE email = 'admin@tourpackage.com')),
    ('Ibex Homestay, Hanle', 'ibex-homestay-hanle',
     'Budget — homestay under Hanle''s night sky.',
     'A budget homestay in Hanle, ideal for stargazers travelling on a smaller budget.',
     3, (SELECT id FROM cities WHERE slug = 'hanle'), 'Hanle Village',
     0.00, 'INR', FALSE, 'PUBLISHED', (SELECT id FROM admins WHERE email = 'admin@tourpackage.com')),
    ('Crane Guest House', 'crane-guest-house-tso-moriri',
     'Budget — guest house near Tso Moriri.',
     'A budget guest house near the Tso Moriri lake on the Changthang plateau.',
     3, (SELECT id FROM cities WHERE slug = 'tso-moriri'), 'Korzok, Tso Moriri',
     0.00, 'INR', FALSE, 'PUBLISHED', (SELECT id FROM admins WHERE email = 'admin@tourpackage.com'));

-- =====================================================================
-- 5. Cover image for each hotel
-- =====================================================================
INSERT INTO hotel_images (hotel_id, url, alt_text, is_cover)
SELECT (SELECT id FROM hotels WHERE slug = v.slug), v.url, v.alt, TRUE
FROM (VALUES
    ('solang-valley-resort-manali', 'https://images.unsplash.com/photo-1782317341310-335b55dc6537?w=1200&h=800&fit=crop&auto=format&q=80', 'Solang Valley Resort in the mountains'),
    ('ayim-hotel-saboo-leh', 'https://images.unsplash.com/photo-1668966780008-b7dae98f61f3?w=1200&h=800&fit=crop&auto=format&q=80', 'Ayim Hotel near Leh'),
    ('gurudi-retreat-hunder-nubra', 'https://images.unsplash.com/photo-1686993999944-f7fd395cdcf7?w=1200&h=800&fit=crop&auto=format&q=80', 'Gurudi Retreat in Hunder'),
    ('the-grand-pangong', 'https://images.unsplash.com/photo-1660303954454-cc270a0d48c4?w=1200&h=800&fit=crop&auto=format&q=80', 'The Grand Pangong by the lake'),
    ('barley-cabin-merak', 'https://images.unsplash.com/photo-1757007813494-f9e7b880d2c6?w=1200&h=800&fit=crop&auto=format&q=80', 'Barley Cabin at Merak'),
    ('the-mor-hanle', 'https://images.unsplash.com/photo-1656937693729-8dc58b9d6c74?w=1200&h=800&fit=crop&auto=format&q=80', 'Night sky over Hanle'),
    ('wisdom-house-tso-moriri', 'https://images.unsplash.com/photo-1583602415713-a0319a0188c0?w=1200&h=800&fit=crop&auto=format&q=80', 'Wisdom House near Tso Moriri'),
    ('ibex-hotel-manali', 'https://images.unsplash.com/photo-1735909600958-f442498875f9?w=1200&h=800&fit=crop&auto=format&q=80', 'Ibex Hotel in Manali'),
    ('ibex-hotel-jispa', 'https://images.unsplash.com/photo-1735909600958-f442498875f9?w=1200&h=800&fit=crop&auto=format&q=80', 'Ibex Hotel in Jispa'),
    ('dorjey-camp-sarchu', 'https://images.unsplash.com/photo-1583602415713-a0319a0188c0?w=1200&h=800&fit=crop&auto=format&q=80', 'Dorjey Camp at Sarchu'),
    ('tara-hotel-leh', 'https://images.unsplash.com/photo-1668966780008-b7dae98f61f3?w=1200&h=800&fit=crop&auto=format&q=80', 'Tara Hotel in Leh'),
    ('summer-camp-nubra', 'https://images.unsplash.com/photo-1686993999944-f7fd395cdcf7?w=1200&h=800&fit=crop&auto=format&q=80', 'Summer Camp in Nubra'),
    ('hidden-heaven-merak', 'https://images.unsplash.com/photo-1757007813494-f9e7b880d2c6?w=1200&h=800&fit=crop&auto=format&q=80', 'Hidden Heaven at Merak'),
    ('ibex-homestay-hanle', 'https://images.unsplash.com/photo-1656937693729-8dc58b9d6c74?w=1200&h=800&fit=crop&auto=format&q=80', 'Ibex Homestay under Hanle skies'),
    ('crane-guest-house-tso-moriri', 'https://images.unsplash.com/photo-1583602415713-a0319a0188c0?w=1200&h=800&fit=crop&auto=format&q=80', 'Crane Guest House near Tso Moriri')
) AS v(slug, url, alt);

-- =====================================================================
-- 6. Amenities — a core set for every hotel, with extras for the premium
--    (5-star) tier. Reuses the amenity reference rows seeded in V14. Rooms
--    are deliberately not seeded until rates are set.
-- =====================================================================
INSERT INTO hotel_amenities (hotel_id, amenity_id)
SELECT h.id, a.id FROM hotels h
JOIN amenities a ON a.slug IN ('free-wifi', 'air-conditioning', '24-hour-front-desk', 'free-parking', 'restaurant')
WHERE NOT EXISTS (SELECT 1 FROM hotel_amenities ha WHERE ha.hotel_id = h.id);

INSERT INTO hotel_amenities (hotel_id, amenity_id)
SELECT h.id, a.id FROM hotels h
JOIN amenities a ON a.slug IN ('airport-shuttle', 'spa-wellness', 'bar-lounge')
WHERE h.star_rating = 5
  AND NOT EXISTS (SELECT 1 FROM hotel_amenities ha WHERE ha.hotel_id = h.id AND ha.amenity_id = a.id);
