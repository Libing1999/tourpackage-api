-- Replaces the placeholder Ladakh packages seeded in V21 with the operator's
-- real, priced tour packages (9 of them), including their day-by-day
-- itineraries and inclusion/exclusion line items exactly as supplied.
--
-- Prices are in INR (the packages carry currency_code = 'INR', which the web
-- app now renders as ₹ on cards and detail pages). Images are the free-licence
-- Unsplash Ladakh photos already used elsewhere in the seed, each verified.
--
-- Testimonials that referenced the old placeholder packages keep working:
-- their package_id FK is ON DELETE SET NULL, so they survive detached.

-- =====================================================================
-- 1. Remove the V21 placeholder packages (cascades itinerary, images,
--    includes, excludes).
-- =====================================================================
DELETE FROM tour_packages WHERE slug IN (
    'leh-ladakh-complete-circuit', 'nubra-valley-pangong-explorer', 'leh-sightseeing-getaway',
    'zanskar-valley-adventure', 'pangong-lake-camping-escape', 'tso-moriri-changthang-expedition',
    'sham-valley-cultural-trail', 'kargil-drass-heritage-tour', 'turtuk-nubra-hidden-villages');

-- =====================================================================
-- 2. Insert the real packages
-- =====================================================================
INSERT INTO tour_packages (title, slug, summary, description, country_id, city_id, duration_days,
                           duration_nights, price, discount_price, currency_code, difficulty_level,
                           rating_average, rating_count, is_featured, status, created_by) VALUES
    ('Leh Tour 5 Nights 6 Days', 'leh-tour-5n-6d',
     'Leh (2N) · Nubra (1N) · Pangong Tso (1N) · Leh (1N)',
     'The classic Ladakh circuit by road — acclimatise in Leh, cross Khardung La into the Nubra Valley, and take in Pangong Tso before returning to Leh. Accommodation with breakfast and dinner, transport, and permits included.',
     (SELECT id FROM countries WHERE iso2 = 'IN'), (SELECT id FROM cities WHERE slug = 'leh'),
     6, 5, 15500.00, NULL, 'INR', 'EASY', 4.70, 214, TRUE, 'PUBLISHED', (SELECT id FROM admins WHERE email = 'admin@tourpackage.com')),
    ('Leh Bike Trip 5 Nights 6 Days', 'leh-bike-trip-5n-6d',
     'Leh (2N) · Nubra (1N) · Pangong Tso (1N) · Leh (1N)',
     'The classic circuit on two wheels — bike with helmets handed over for four days, Khardung La, the Nubra dunes, and Pangong Tso. Accommodation with breakfast and dinner, transport, and permits included.',
     (SELECT id FROM countries WHERE iso2 = 'IN'), (SELECT id FROM cities WHERE slug = 'leh'),
     6, 5, 17500.00, NULL, 'INR', 'MODERATE', 4.60, 168, FALSE, 'PUBLISHED', (SELECT id FROM admins WHERE email = 'admin@tourpackage.com')),
    ('Leh–Nubra–Turtuk–Pangong 6 Nights 7 Days', 'leh-nubra-turtuk-pangong-6n-7d',
     'Leh (2N) · Nubra (2N) · Pangong (1N) · Leh (1N)',
     'Adds the Balti village of Turtuk near the border to the classic circuit. Free airport pick-up and drop, accommodation (2 adults sharing) with breakfast and dinner, a separate car per the itinerary, and permits.',
     (SELECT id FROM countries WHERE iso2 = 'IN'), (SELECT id FROM cities WHERE slug = 'nubra-valley'),
     7, 6, 18500.00, NULL, 'INR', 'MODERATE', 4.80, 156, TRUE, 'PUBLISHED', (SELECT id FROM admins WHERE email = 'admin@tourpackage.com')),
    ('Leh Bike Tour 6 Nights 7 Days', 'leh-bike-tour-6n-7d',
     'Leh (2N) · Nubra (2N) · Pangong Tso (1N) · Leh (1N)',
     'The seven-day circuit on a bike, taking in Turtuk beyond Nubra. Bike with helmets, accommodation with breakfast and dinner, transport, and permits included.',
     (SELECT id FROM countries WHERE iso2 = 'IN'), (SELECT id FROM cities WHERE slug = 'nubra-valley'),
     7, 6, 19500.00, NULL, 'INR', 'MODERATE', 4.70, 102, FALSE, 'PUBLISHED', (SELECT id FROM admins WHERE email = 'admin@tourpackage.com')),
    ('Leh–Nubra–Pangong–Tso Moriri 7 Nights 8 Days', 'leh-nubra-pangong-tsomoriri-7n-8d',
     'Leh (2N) · Nubra (2N) · Pangong (1N) · Tso Moriri (1N) · Leh (1N)',
     'Extends the circuit south to the high-altitude Tso Moriri lake on the Changthang plateau. Accommodation with breakfast and dinner, a separate car per the itinerary, and permits.',
     (SELECT id FROM countries WHERE iso2 = 'IN'), (SELECT id FROM cities WHERE slug = 'tso-moriri'),
     8, 7, 23450.00, NULL, 'INR', 'MODERATE', 4.80, 88, FALSE, 'PUBLISHED', (SELECT id FROM admins WHERE email = 'admin@tourpackage.com')),
    ('Leh Ladakh 8 Nights 9 Days', 'leh-ladakh-8n-9d',
     'Leh (2N) · Nubra (2N) · Pangong (1N) · Hanle (1N) · Tso Moriri (1N) · Leh (1N)',
     'The grand tour — Turtuk, Pangong, the dark skies and observatory of Hanle, and Tso Moriri. Accommodation with breakfast and dinner, a separate car per the itinerary, and permits.',
     (SELECT id FROM countries WHERE iso2 = 'IN'), (SELECT id FROM cities WHERE slug = 'pangong-lake'),
     9, 8, 25000.00, NULL, 'INR', 'CHALLENGING', 4.90, 74, TRUE, 'PUBLISHED', (SELECT id FROM admins WHERE email = 'admin@tourpackage.com')),
    ('Leh Ladakh Bike Trip — Umling La', 'leh-ladakh-bike-trip-umling-la',
     'Leh (2N) · Nubra (1N) · Pangong (1N) · Hanle (1N) · Umling La (1N) · Leh (1N)',
     'A supported bike expedition to Umling La, the world''s highest motorable road, via Hanle and Demchok. Includes bike, fuel, oxygen cylinder, trained mechanic, ride captain, riding gear, first-aid kit and spares, with meals and permits.',
     (SELECT id FROM countries WHERE iso2 = 'IN'), (SELECT id FROM cities WHERE slug = 'pangong-lake'),
     8, 7, 27990.00, NULL, 'INR', 'CHALLENGING', 4.90, 63, FALSE, 'PUBLISHED', (SELECT id FROM admins WHERE email = 'admin@tourpackage.com')),
    ('Leh Ladakh via Road', 'leh-ladakh-via-road',
     'Srinagar (1N) · Kargil (1N) · Leh (1N) · Nubra (2N) · Pangong (1N) · Leh (1N) · Sarchu (1N) · Manali (1N)',
     'The full overland journey from Srinagar to Manali across the Himalayas — Sonmarg, Zoji La, Kargil, the Leh circuit, and the Leh–Manali highway. Accommodation with breakfast and dinner, transport and sightseeing, and permits.',
     (SELECT id FROM countries WHERE iso2 = 'IN'), (SELECT id FROM cities WHERE slug = 'kargil'),
     10, 9, 34999.00, NULL, 'INR', 'CHALLENGING', 4.80, 57, TRUE, 'PUBLISHED', (SELECT id FROM admins WHERE email = 'admin@tourpackage.com')),
    ('Manali to Leh Bike Tour 8 Nights 9 Days', 'manali-to-leh-bike-tour-8n-9d',
     'Manali (1N) · Sarchu (1N) · Leh (2N) · Nubra (2N) · Pangong (1N) · Leh (1N) — ₹39,500 dual / ₹47,500 solo rider',
     'The legendary Manali–Leh ride over the Gata Loops and Nakee La, then the full Leh circuit. Accommodation with breakfast and dinner, transport and sightseeing, and permits. Priced ₹39,500 for a dual rider; ₹47,500 for a solo rider.',
     (SELECT id FROM countries WHERE iso2 = 'IN'), (SELECT id FROM cities WHERE slug = 'leh'),
     9, 8, 39500.00, NULL, 'INR', 'CHALLENGING', 4.90, 91, FALSE, 'PUBLISHED', (SELECT id FROM admins WHERE email = 'admin@tourpackage.com'));

-- =====================================================================
-- 3. Cover images (+ a couple of extra gallery images per package)
-- =====================================================================
INSERT INTO package_images (package_id, url, alt_text, is_cover, display_order) VALUES
    ((SELECT id FROM tour_packages WHERE slug = 'leh-tour-5n-6d'),
     'https://images.unsplash.com/photo-1668966780008-b7dae98f61f3?w=1200&h=800&fit=crop&auto=format&q=80', 'Leh town below the mountains', TRUE, 0),
    ((SELECT id FROM tour_packages WHERE slug = 'leh-tour-5n-6d'),
     'https://images.unsplash.com/photo-1660303954454-cc270a0d48c4?w=1200&h=800&fit=crop&auto=format&q=80', 'Pangong Tso', FALSE, 1),
    ((SELECT id FROM tour_packages WHERE slug = 'leh-bike-trip-5n-6d'),
     'https://images.unsplash.com/photo-1686993999944-f7fd395cdcf7?w=1200&h=800&fit=crop&auto=format&q=80', 'Riding the Ladakh road', TRUE, 0),
    ((SELECT id FROM tour_packages WHERE slug = 'leh-bike-trip-5n-6d'),
     'https://images.unsplash.com/photo-1660303954454-cc270a0d48c4?w=1200&h=800&fit=crop&auto=format&q=80', 'Pangong Tso', FALSE, 1),
    ((SELECT id FROM tour_packages WHERE slug = 'leh-nubra-turtuk-pangong-6n-7d'),
     'https://images.unsplash.com/photo-1660303954454-cc270a0d48c4?w=1200&h=800&fit=crop&auto=format&q=80', 'Pangong Tso and the ranges', TRUE, 0),
    ((SELECT id FROM tour_packages WHERE slug = 'leh-nubra-turtuk-pangong-6n-7d'),
     'https://images.unsplash.com/photo-1782317341310-335b55dc6537?w=1200&h=800&fit=crop&auto=format&q=80', 'A monastery near Turtuk', FALSE, 1),
    ((SELECT id FROM tour_packages WHERE slug = 'leh-bike-tour-6n-7d'),
     'https://images.unsplash.com/photo-1757007813494-f9e7b880d2c6?w=1200&h=800&fit=crop&auto=format&q=80', 'Pangong Tso with prayer flags', TRUE, 0),
    ((SELECT id FROM tour_packages WHERE slug = 'leh-bike-tour-6n-7d'),
     'https://images.unsplash.com/photo-1686993999944-f7fd395cdcf7?w=1200&h=800&fit=crop&auto=format&q=80', 'Riding the Ladakh road', FALSE, 1),
    ((SELECT id FROM tour_packages WHERE slug = 'leh-nubra-pangong-tsomoriri-7n-8d'),
     'https://images.unsplash.com/photo-1583602415713-a0319a0188c0?w=1200&h=800&fit=crop&auto=format&q=80', 'A high-plateau lake at Tso Moriri', TRUE, 0),
    ((SELECT id FROM tour_packages WHERE slug = 'leh-nubra-pangong-tsomoriri-7n-8d'),
     'https://images.unsplash.com/photo-1660303954454-cc270a0d48c4?w=1200&h=800&fit=crop&auto=format&q=80', 'Pangong Tso', FALSE, 1),
    ((SELECT id FROM tour_packages WHERE slug = 'leh-ladakh-8n-9d'),
     'https://images.unsplash.com/photo-1656937693729-8dc58b9d6c74?w=1200&h=800&fit=crop&auto=format&q=80', 'Night sky over Ladakh at Hanle', TRUE, 0),
    ((SELECT id FROM tour_packages WHERE slug = 'leh-ladakh-8n-9d'),
     'https://images.unsplash.com/photo-1583602415713-a0319a0188c0?w=1200&h=800&fit=crop&auto=format&q=80', 'Tso Moriri lake', FALSE, 1),
    ((SELECT id FROM tour_packages WHERE slug = 'leh-ladakh-bike-trip-umling-la'),
     'https://images.unsplash.com/photo-1686993999944-f7fd395cdcf7?w=1200&h=800&fit=crop&auto=format&q=80', 'Riding to Umling La', TRUE, 0),
    ((SELECT id FROM tour_packages WHERE slug = 'leh-ladakh-bike-trip-umling-la'),
     'https://images.unsplash.com/photo-1656937693729-8dc58b9d6c74?w=1200&h=800&fit=crop&auto=format&q=80', 'Dark skies at Hanle', FALSE, 1),
    ((SELECT id FROM tour_packages WHERE slug = 'leh-ladakh-via-road'),
     'https://images.unsplash.com/photo-1735909600958-f442498875f9?w=1200&h=800&fit=crop&auto=format&q=80', 'The overland route through the ranges', TRUE, 0),
    ((SELECT id FROM tour_packages WHERE slug = 'leh-ladakh-via-road'),
     'https://images.unsplash.com/photo-1668966780008-b7dae98f61f3?w=1200&h=800&fit=crop&auto=format&q=80', 'Leh', FALSE, 1),
    ((SELECT id FROM tour_packages WHERE slug = 'manali-to-leh-bike-tour-8n-9d'),
     'https://images.unsplash.com/photo-1686993999944-f7fd395cdcf7?w=1200&h=800&fit=crop&auto=format&q=80', 'The Manali–Leh ride', TRUE, 0),
    ((SELECT id FROM tour_packages WHERE slug = 'manali-to-leh-bike-tour-8n-9d'),
     'https://images.unsplash.com/photo-1757007813494-f9e7b880d2c6?w=1200&h=800&fit=crop&auto=format&q=80', 'Pangong Tso', FALSE, 1);

-- =====================================================================
-- 4. Itineraries (day-by-day, as supplied)
-- =====================================================================
INSERT INTO package_itinerary (package_id, day_number, title, description, meals, accommodation)
SELECT (SELECT id FROM tour_packages WHERE slug = v.slug), v.day, v.title, v.descr, v.meals, v.accom
FROM (VALUES
    -- 1) Leh Tour 5N/6D
    ('leh-tour-5n-6d', 1, 'Arrival in Leh — Shanti Stupa & Leh Palace', 'Experience a wonderful flight over the Himalayas. On arrival, you are received by our representative and transferred to the hotel. After welcome tea and coffee, rest for the day to acclimatise. In the evening, visit Shanti Stupa and Leh Palace. Overnight stay in the hotel.', 'Dinner', 'Leh Hotel'),
    ('leh-tour-5n-6d', 2, 'Hall of Fame / Magnetic Hill / Gurudwara / Sangam Point', 'After breakfast, an excursion to the Hall of Fame (the Kargil War Memorial), Gurudwara Pathar Sahib, Magnetic Hill, Kali Temple and Sangam Point — where the Zanskar and Indus rivers meet at Nimu. Enjoy optional rafting on the Zanskar river. In the evening, return to Leh. Overnight stay in the hotel.', 'Breakfast, Dinner', 'Leh Hotel'),
    ('leh-tour-5n-6d', 3, 'Leh to Nubra Valley via Khardung La (125 km)', 'After an early breakfast, drive to the beautiful Nubra Valley via Khardung La (among the highest motorable roads in the world, 18,380 ft) and on to Hunder. On arrival, check in to the camp; in the afternoon enjoy the dunes and the double-humped Bactrian camels. Overnight stay in camp.', 'Breakfast, Dinner', 'Nubra Camp'),
    ('leh-tour-5n-6d', 4, 'Diskit / Hunder to Pangong Lake (via Shyok)', 'Rise early to visit Diskit Monastery for a wonderful view over the Nubra Valley, then drive to Pangong Lake via Shyok. Take in the turquoise panoramic lake and its magnificent mountains and landscapes. Dinner and overnight at camp.', 'Breakfast, Dinner', 'Pangong Camp'),
    ('leh-tour-5n-6d', 5, 'Pangong Lake to Leh', 'After early breakfast, begin the journey back to Leh via Chang La. On the way, visit Thiksey Monastery and the Rancho School. Dinner and overnight stay in a Leh hotel.', 'Breakfast, Dinner', 'Leh Hotel'),
    ('leh-tour-5n-6d', 6, 'Departure', 'After breakfast, transfer to the airport for your onward flight, with sweet memories of Leh.', 'Breakfast', NULL),

    -- 2) Leh Bike Trip 5N/6D
    ('leh-bike-trip-5n-6d', 1, 'Arrival in Leh — Shanti Stupa & Leh Palace', 'Experience a wonderful flight over the Himalayas. On arrival, you are received by our representative and transferred to the hotel. After welcome tea and coffee, rest for the day to acclimatise. In the evening, visit Shanti Stupa and Leh Palace. Overnight stay in the hotel.', 'Dinner', 'Leh Hotel'),
    ('leh-bike-trip-5n-6d', 2, 'Hall of Fame / Magnetic Hill / Gurudwara / Sangam Point', 'After breakfast, the bike is handed over for four days. Excursion to the Hall of Fame (the Kargil War Memorial), Gurudwara Pathar Sahib, Magnetic Hill, Kali Temple and Sangam Point at Nimu, where the Zanskar and Indus rivers meet. Enjoy optional rafting on the Zanskar river. In the evening, return to Leh. Overnight stay in the hotel.', 'Breakfast, Dinner', 'Leh Hotel'),
    ('leh-bike-trip-5n-6d', 3, 'Leh to Nubra Valley via Khardung La (125 km)', 'After an early breakfast, ride to the beautiful Nubra Valley via Khardung La (among the highest motorable roads in the world, 18,380 ft) and on to Hunder. On arrival, check in to the camp; in the afternoon enjoy the dunes and the double-humped Bactrian camels. Overnight stay in camp.', 'Breakfast, Dinner', 'Nubra Camp'),
    ('leh-bike-trip-5n-6d', 4, 'Diskit / Hunder to Pangong Lake (via Shyok)', 'Rise early to visit Diskit Monastery for a wonderful view over the Nubra Valley, then ride to Pangong Lake via Shyok. Take in the turquoise panoramic lake and its magnificent mountains and landscapes. Dinner and overnight at camp.', 'Breakfast, Dinner', 'Pangong Camp'),
    ('leh-bike-trip-5n-6d', 5, 'Pangong Lake to Leh', 'After early breakfast, ride back to Leh via Chang La. On the way, visit Thiksey Monastery and the Rancho School. Dinner and overnight stay in a Leh hotel.', 'Breakfast, Dinner', 'Leh Hotel'),
    ('leh-bike-trip-5n-6d', 6, 'Departure', 'After breakfast, transfer to the airport for your onward flight, with sweet memories of Leh.', 'Breakfast', NULL),

    -- 3) Leh-Nubra-Turtuk-Pangong 6N/7D
    ('leh-nubra-turtuk-pangong-6n-7d', 1, 'Arrival in Leh — Shanti Stupa', 'Experience a wonderful flight over the Himalayas. On arrival, you are received by our representative and transferred to the hotel. After welcome tea and coffee, rest for the day. In the afternoon, if health permits, drive to Shanti Stupa. Overnight stay in the hotel.', 'Dinner', 'Leh Hotel'),
    ('leh-nubra-turtuk-pangong-6n-7d', 2, 'Hall of Fame / Magnetic Hill / Gurudwara / Sangam Point', 'After breakfast, an excursion to the Hall of Fame (the Kargil War Memorial), Gurudwara Pathar Sahib, Magnetic Hill, Kali Temple and Sangam Point at Nimu, where the Zanskar and Indus rivers meet. Enjoy optional rafting on the Zanskar river. In the evening, return to Leh. Overnight stay in the hotel.', 'Breakfast, Dinner', 'Leh Hotel'),
    ('leh-nubra-turtuk-pangong-6n-7d', 3, 'Leh to Nubra Valley via Khardung La (125 km)', 'After an early breakfast, drive to the beautiful Nubra Valley via Khardung La (among the highest motorable roads in the world, 18,380 ft) and on to Hunder. On arrival, check in to the camp; in the afternoon enjoy the dunes and the double-humped Bactrian camels. Overnight stay in camp.', 'Breakfast, Dinner', 'Nubra Camp'),
    ('leh-nubra-turtuk-pangong-6n-7d', 4, 'Nubra Valley to Turtuk', 'After breakfast, drive to Turtuk for an excursion of the Turtuk village near the border area, then return to Nubra. Overnight stay in Nubra.', 'Breakfast, Dinner', 'Nubra Camp'),
    ('leh-nubra-turtuk-pangong-6n-7d', 5, 'Diskit / Hunder to Pangong Lake (via Shyok)', 'Rise early to visit Diskit Monastery for a wonderful view over the Nubra Valley, then drive to Pangong Lake via Shyok. Take in the turquoise panoramic lake and its magnificent landscapes. Dinner and overnight at camp.', 'Breakfast, Dinner', 'Pangong Camp'),
    ('leh-nubra-turtuk-pangong-6n-7d', 6, 'Pangong Lake to Leh — free evening', 'After early breakfast, begin the journey back to Leh via Chang La. On the way, visit Thiksey Monastery and the Rancho School. The rest of the day is free to stroll around the Leh market. Overnight stay in Leh.', 'Breakfast, Dinner', 'Leh Hotel'),
    ('leh-nubra-turtuk-pangong-6n-7d', 7, 'Departure', 'After breakfast, transfer to the airport for your onward flight, with sweet memories of Leh.', 'Breakfast', NULL),

    -- 4) Leh Bike Tour 6N/7D
    ('leh-bike-tour-6n-7d', 1, 'Arrival in Leh — Shanti Stupa & Leh Palace', 'Experience a wonderful flight over the Himalayas. On arrival, you are received by our representative and transferred to the hotel. After welcome tea and coffee, rest for the day to acclimatise. In the evening, visit Shanti Stupa and Leh Palace. Overnight stay in the hotel.', 'Dinner', 'Leh Hotel'),
    ('leh-bike-tour-6n-7d', 2, 'Hall of Fame / Magnetic Hill / Gurudwara / Sangam Point', 'After breakfast, an excursion to the Hall of Fame (the Kargil War Memorial), Gurudwara Pathar Sahib, Magnetic Hill, Kali Temple and Sangam Point at Nimu, where the Zanskar and Indus rivers meet. Enjoy optional rafting on the Zanskar river. In the evening, return to Leh. Overnight stay in the hotel.', 'Breakfast, Dinner', 'Leh Hotel'),
    ('leh-bike-tour-6n-7d', 3, 'Leh to Nubra Valley via Khardung La (125 km)', 'After an early breakfast, ride to the beautiful Nubra Valley via Khardung La (among the highest motorable roads in the world, 18,380 ft) and on to Hunder. On arrival, check in to the camp; in the afternoon enjoy the dunes and the double-humped Bactrian camels. Overnight stay in camp.', 'Breakfast, Dinner', 'Nubra Camp'),
    ('leh-bike-tour-6n-7d', 4, 'Nubra to Turtuk', 'After breakfast, ride to Turtuk for an excursion of the Turtuk village near the border area, then return to Nubra. Overnight stay in Nubra.', 'Breakfast, Dinner', 'Nubra Camp'),
    ('leh-bike-tour-6n-7d', 5, 'Diskit / Hunder to Pangong Lake (via Shyok)', 'Rise early to visit Diskit Monastery for a wonderful view over the Nubra Valley, then ride to Pangong Lake via Shyok. Take in the turquoise panoramic lake and its magnificent landscapes. Dinner and overnight at camp.', 'Breakfast, Dinner', 'Pangong Camp'),
    ('leh-bike-tour-6n-7d', 6, 'Pangong Lake to Leh', 'After early breakfast, ride back to Leh via Chang La. On the way, visit Thiksey Monastery and the Rancho School. Dinner and overnight stay in a Leh hotel.', 'Breakfast, Dinner', 'Leh Hotel'),
    ('leh-bike-tour-6n-7d', 7, 'Departure', 'After breakfast, transfer to the airport for your onward flight, with sweet memories of Leh.', 'Breakfast', NULL),

    -- 5) Leh-Nubra-Pangong-Tsomoriri 7N/8D
    ('leh-nubra-pangong-tsomoriri-7n-8d', 1, 'Arrival in Leh — Shanti Stupa & Leh Palace', 'Experience a wonderful flight over the Himalayas. On arrival, you are received by our representative and transferred to the hotel. After welcome tea and coffee, rest for the day to acclimatise. In the evening, visit Shanti Stupa and Leh Palace. Overnight stay in the hotel.', 'Dinner', 'Leh Hotel'),
    ('leh-nubra-pangong-tsomoriri-7n-8d', 2, 'Hall of Fame / Magnetic Hill / Gurudwara / Sangam Point', 'After breakfast, an excursion to the Hall of Fame (the Kargil War Memorial), Gurudwara Pathar Sahib, Magnetic Hill, Kali Temple and Sangam Point at Nimu, where the Zanskar and Indus rivers meet. Enjoy optional rafting on the Zanskar river. In the evening, return to Leh. Overnight stay in the hotel.', 'Breakfast, Dinner', 'Leh Hotel'),
    ('leh-nubra-pangong-tsomoriri-7n-8d', 3, 'Leh to Nubra Valley via Khardung La (125 km)', 'After an early breakfast, drive to the beautiful Nubra Valley via Khardung La (among the highest motorable roads in the world, 18,380 ft) and on to Hunder. On arrival, check in to the camp; in the afternoon enjoy the dunes and the double-humped Bactrian camels. Overnight stay in camp.', 'Breakfast, Dinner', 'Nubra Camp'),
    ('leh-nubra-pangong-tsomoriri-7n-8d', 4, 'Nubra Valley to Turtuk', 'After breakfast, drive to Turtuk for an excursion of the Turtuk village near the border area, then return to Nubra. Overnight stay in Nubra.', 'Breakfast, Dinner', 'Nubra Camp'),
    ('leh-nubra-pangong-tsomoriri-7n-8d', 5, 'Diskit / Hunder to Pangong Lake (via Shyok)', 'Rise early to visit Diskit Monastery for a wonderful view over the Nubra Valley, then drive to Pangong Lake via Shyok. Take in the turquoise panoramic lake and its magnificent landscapes. Dinner and overnight at camp.', 'Breakfast, Dinner', 'Pangong Camp'),
    ('leh-nubra-pangong-tsomoriri-7n-8d', 6, 'Pangong Lake to Tso Moriri', 'After early breakfast, begin the journey towards Tso Moriri lake. On the way, enjoy the beautiful textures of mountains unlike any seen before. On arrival, check in to the hotel and take in an excursion of Tso Moriri lake. Overnight stay in the hotel.', 'Breakfast, Dinner', 'Tso Moriri Hotel'),
    ('leh-nubra-pangong-tsomoriri-7n-8d', 7, 'Tso Moriri to Leh', 'After breakfast, return to Leh. Overnight stay in Leh.', 'Breakfast, Dinner', 'Leh Hotel'),
    ('leh-nubra-pangong-tsomoriri-7n-8d', 8, 'Departure', 'After breakfast, transfer to the airport for your onward flight, with sweet memories of Leh.', 'Breakfast', NULL),

    -- 6) Leh Ladakh 8N/9D
    ('leh-ladakh-8n-9d', 1, 'Arrival in Leh — Shanti Stupa & Leh Palace', 'Experience a wonderful flight over the Himalayas. On arrival, you are received by our representative and transferred to the hotel. After welcome tea and coffee, rest for the day to acclimatise. In the evening, visit Shanti Stupa and Leh Palace. Overnight stay in the hotel.', 'Dinner', 'Leh Hotel'),
    ('leh-ladakh-8n-9d', 2, 'Hall of Fame / Magnetic Hill / Gurudwara / Sangam Point', 'After breakfast, an excursion to the Hall of Fame (the Kargil War Memorial), Gurudwara Pathar Sahib, Magnetic Hill, Kali Temple and Sangam Point at Nimu, where the Zanskar and Indus rivers meet. Enjoy optional rafting on the Zanskar river. In the evening, return to Leh. Overnight stay in the hotel.', 'Breakfast, Dinner', 'Leh Hotel'),
    ('leh-ladakh-8n-9d', 3, 'Leh to Nubra Valley via Khardung La (125 km)', 'After an early breakfast, drive to the beautiful Nubra Valley via Khardung La (among the highest motorable roads in the world, 18,380 ft) and on to Hunder. On arrival, check in to the camp; in the afternoon enjoy the dunes and the double-humped Bactrian camels. Overnight stay in camp.', 'Breakfast, Dinner', 'Nubra Camp'),
    ('leh-ladakh-8n-9d', 4, 'Nubra Valley to Turtuk', 'After breakfast, drive to Turtuk for an excursion of the Turtuk village near the border area, then return to Nubra. Overnight stay in Nubra.', 'Breakfast, Dinner', 'Nubra Camp'),
    ('leh-ladakh-8n-9d', 5, 'Diskit / Hunder to Pangong Lake (via Shyok)', 'Rise early to visit Diskit Monastery for a wonderful view over the Nubra Valley, then drive to Pangong Lake via Shyok. Take in the turquoise panoramic lake and its magnificent landscapes. Dinner and overnight at camp.', 'Breakfast, Dinner', 'Pangong Camp'),
    ('leh-ladakh-8n-9d', 6, 'Pangong Lake to Hanle', 'After early breakfast, begin the journey towards Hanle, famous for its large terrestrial telescope and its extraordinary night skies. Overnight stay in Hanle.', 'Breakfast, Dinner', 'Hanle Guesthouse'),
    ('leh-ladakh-8n-9d', 7, 'Hanle to Tso Moriri', 'After early breakfast, begin the journey towards Tso Moriri lake, enjoying the mountain landscapes on the way. On arrival, check in to the hotel and take in an excursion of Tso Moriri lake. Overnight stay in the hotel.', 'Breakfast, Dinner', 'Tso Moriri Hotel'),
    ('leh-ladakh-8n-9d', 8, 'Tso Moriri to Leh', 'After breakfast, return to Leh. Overnight stay in Leh.', 'Breakfast, Dinner', 'Leh Hotel'),
    ('leh-ladakh-8n-9d', 9, 'Departure', 'After breakfast, transfer to the airport for your onward flight, with sweet memories of Leh.', 'Breakfast', NULL),

    -- 7) Leh Ladakh Bike Trip — Umling La (8D)
    ('leh-ladakh-bike-trip-umling-la', 1, 'Arrival in Leh — Shanti Stupa & Leh Palace', 'Experience a wonderful flight over the Himalayas. On arrival, you are received by our representative and transferred to the hotel. After welcome tea and coffee, rest for the day to acclimatise. In the evening, visit Shanti Stupa and Leh Palace. Overnight stay in the hotel.', 'Dinner', 'Leh Hotel'),
    ('leh-ladakh-bike-trip-umling-la', 2, 'Hall of Fame / Magnetic Hill / Gurudwara / Sangam Point', 'After breakfast, an excursion to the Hall of Fame (the Kargil War Memorial), Gurudwara Pathar Sahib, Magnetic Hill, Kali Temple and Sangam Point at Nimu, where the Zanskar and Indus rivers meet. Enjoy optional rafting on the Zanskar river. In the evening, return to Leh. Overnight stay in the hotel.', 'Breakfast, Dinner', 'Leh Hotel'),
    ('leh-ladakh-bike-trip-umling-la', 3, 'Leh to Nubra Valley via Khardung La (125 km)', 'After an early breakfast, ride to the beautiful Nubra Valley via Khardung La (among the highest motorable roads in the world, 18,380 ft) and on to Hunder. On arrival, check in to the camp; in the afternoon enjoy the dunes and the double-humped Bactrian camels. Overnight stay in camp.', 'Breakfast, Dinner', 'Nubra Camp'),
    ('leh-ladakh-bike-trip-umling-la', 4, 'Diskit / Hunder to Pangong Lake (via Shyok)', 'Rise early to visit Diskit Monastery for a wonderful view over the Nubra Valley, then ride to Pangong Lake via Shyok. Take in the turquoise panoramic lake and its magnificent landscapes. Dinner and overnight at camp.', 'Breakfast, Dinner', 'Pangong Camp'),
    ('leh-ladakh-bike-trip-umling-la', 5, 'Pangong Lake to Hanle', 'After early breakfast, ride towards Hanle. Visit the beautiful meadows of Hanle and the world''s second-highest space observatory. Overnight stay in a guesthouse.', 'Breakfast, Dinner', 'Hanle Guesthouse'),
    ('leh-ladakh-bike-trip-umling-la', 6, 'Hanle to Umling La', 'After breakfast, ride to Umling La — the world''s highest motorable road — and on to Demchok village, the actual India–China border village. Overnight stay near Hanle.', 'Breakfast, Dinner', 'Hanle Guesthouse'),
    ('leh-ladakh-bike-trip-umling-la', 7, 'Back to Leh', 'After breakfast, ride back to Leh, visiting the Chumathang hot water springs, Hemis Gompa and Thiksey Monastery on the way. Overnight stay in Leh.', 'Breakfast, Dinner', 'Leh Hotel'),
    ('leh-ladakh-bike-trip-umling-la', 8, 'Departure', 'After breakfast, transfer to the airport for your onward flight, with sweet memories of Leh.', 'Breakfast', NULL),

    -- 8) Leh Ladakh via Road (10D)
    ('leh-ladakh-via-road', 1, 'Jammu to Srinagar', 'Pickup at Jammu and drive to Srinagar. On arrival, check in to the hotel, then an excursion to Srinagar''s famous sights such as Dal Lake and Shalimar Bagh. Overnight stay at the hotel.', 'Dinner', 'Srinagar Hotel'),
    ('leh-ladakh-via-road', 2, 'Srinagar to Kargil', 'After breakfast, transfer to Kargil. On the way, an excursion to Sonmarg, Zoji La Pass and the Drass valley. Overnight stay in Kargil.', 'Breakfast, Dinner', 'Kargil Hotel'),
    ('leh-ladakh-via-road', 3, 'Kargil to Leh', 'After breakfast, visit the Kargil War Memorial and drive to Leh. On the way, enjoy the high Karakoram ranges, the sangam of the Zanskar and Indus rivers, Magnetic Hill and Lamayuru Monastery. On arrival, check in to the hotel. Overnight stay in the hotel.', 'Breakfast, Dinner', 'Leh Hotel'),
    ('leh-ladakh-via-road', 4, 'Leh to Nubra Valley', 'After an early breakfast, drive to the beautiful Nubra Valley via Khardung La (among the highest motorable roads in the world, 18,380 ft) and on to Hunder. On arrival, check in to the camp; in the afternoon enjoy the sand dunes and the double-humped Bactrian camels. Overnight stay in Nubra.', 'Breakfast, Dinner', 'Nubra Camp'),
    ('leh-ladakh-via-road', 5, 'Nubra Valley to Turtuk', 'After breakfast, drive to Turtuk for an excursion of the Turtuk village near the border area, then return to Nubra. Overnight stay in Nubra.', 'Breakfast, Dinner', 'Nubra Camp'),
    ('leh-ladakh-via-road', 6, 'Diskit / Hunder to Pangong', 'After breakfast, visit Diskit Monastery for a wonderful view over the Nubra Valley, then drive to Pangong Lake via Shyok. Take in the turquoise panoramic lake and its magnificent landscapes. Overnight stay at Pangong.', 'Breakfast, Dinner', 'Pangong Camp'),
    ('leh-ladakh-via-road', 7, 'Pangong to Leh', 'After breakfast, return to Leh via Chang La Pass, with an excursion of Shey Monastery and the Rancho School on the way. On arrival in Leh, check in to the hotel. Overnight stay in Leh.', 'Breakfast, Dinner', 'Leh Hotel'),
    ('leh-ladakh-via-road', 8, 'Leh to Sarchu', 'After breakfast, check out and continue the journey to Sarchu, about 215 km away — an essential stopover on the Leh–Manali highway, on the boundary of Himachal Pradesh and Jammu & Kashmir. Overnight stay in Sarchu.', 'Breakfast, Dinner', 'Sarchu Camp'),
    ('leh-ladakh-via-road', 9, 'Sarchu to Manali', 'After the stopover at Sarchu, post breakfast, continue to Manali via the Leh–Manali highway. Overnight stay at the hotel.', 'Breakfast, Dinner', 'Manali Hotel'),
    ('leh-ladakh-via-road', 10, 'Manali to Chandigarh — Departure', 'Post breakfast, drop at Chandigarh airport or railway station.', 'Breakfast', NULL),

    -- 9) Manali to Leh Bike Tour 8N/9D
    ('manali-to-leh-bike-tour-8n-9d', 1, 'Arrival in Manali', 'Arrival in Manali and check in to the hotel. Begin your adventure with a bike from Manali, then sightseeing around Manali. Overnight stay at the hotel.', 'Dinner', 'Manali Hotel'),
    ('manali-to-leh-bike-tour-8n-9d', 2, 'Manali to Sarchu', 'After breakfast, check out and ride to Sarchu, about 215 km away — an important stopover on the Leh–Manali highway, on the boundary of Himachal Pradesh and Jammu & Kashmir. Stay in a camp in Sarchu.', 'Breakfast, Dinner', 'Sarchu Camp'),
    ('manali-to-leh-bike-tour-8n-9d', 3, 'Sarchu to Leh', 'After breakfast, ride towards Leh. En route, witness the barren lands bordered by the lofty Himalayas and ride the Gata Loops — a series of 21 hairpin bends leading to the Nakee La Pass. Night stay in Leh.', 'Breakfast, Dinner', 'Leh Hotel'),
    ('manali-to-leh-bike-tour-8n-9d', 4, 'Sightseeing in Leh', 'After breakfast, visit the Shanti Stupa, built to commemorate 2,500 years of Buddhism, for panoramic views of the city, and the 17th-century Leh Palace. Later, explore the local Leh market for Tibetan handicrafts and cafes. Overnight stay at the hotel in Leh.', 'Breakfast, Dinner', 'Leh Hotel'),
    ('manali-to-leh-bike-tour-8n-9d', 5, 'Leh to Nubra Valley via Khardung La', 'After breakfast, ride to the world''s highest motorable road at Khardung La and descend into the Nubra Valley, where the landscape changes to white sand dunes. If time permits, visit Diskit Monastery with its large Buddha statue and enjoy a double-humped Bactrian camel ride. Overnight stay at the campsite in Nubra Valley.', 'Breakfast, Dinner', 'Nubra Camp'),
    ('manali-to-leh-bike-tour-8n-9d', 6, 'Nubra Valley to Turtuk', 'After breakfast, ride to Turtuk for an excursion of the Turtuk village near the border area, then return to Nubra. Overnight stay in Nubra.', 'Breakfast, Dinner', 'Nubra Camp'),
    ('manali-to-leh-bike-tour-8n-9d', 7, 'Diskit / Hunder to Pangong', 'After breakfast, ride to Pangong Lake on the Indo-China border — one of the highest-altitude saline lakes, changing colour through shades of blue, green and red. Spend the day exploring its surroundings. Overnight stay and dinner at a campsite near Pangong Lake.', 'Breakfast, Dinner', 'Pangong Camp'),
    ('manali-to-leh-bike-tour-8n-9d', 8, 'Pangong to Leh', 'After breakfast, return to Leh via Chang La Pass, with an excursion of Shey Monastery and the Rancho School on the way. On arrival in Leh, check in to the hotel. Overnight stay in Leh.', 'Breakfast, Dinner', 'Leh Hotel'),
    ('manali-to-leh-bike-tour-8n-9d', 9, 'Departure', 'Post breakfast, drop at the airport with lifetime memories.', 'Breakfast', NULL)
) AS v(slug, day, title, descr, meals, accom);

-- =====================================================================
-- 5. Inclusions
-- =====================================================================
INSERT INTO package_includes (package_id, description, icon, display_order)
SELECT (SELECT id FROM tour_packages WHERE slug = v.slug), v.descr, v.icon, v.ord
FROM (VALUES
    ('leh-tour-5n-6d', 'Accommodation with meals (breakfast and dinner)', 'bed-double', 1),
    ('leh-tour-5n-6d', 'Transportation for pick-up/drop-off and sightseeing', 'car', 2),
    ('leh-tour-5n-6d', 'Inner Line Permits', 'ticket', 3),

    ('leh-bike-trip-5n-6d', 'Bike with helmets', 'bike', 1),
    ('leh-bike-trip-5n-6d', 'Accommodation with meals (breakfast and dinner)', 'bed-double', 2),
    ('leh-bike-trip-5n-6d', 'Transportation for pick-up/drop-off and sightseeing', 'car', 3),
    ('leh-bike-trip-5n-6d', 'Inner Line Permits', 'ticket', 4),

    ('leh-nubra-turtuk-pangong-6n-7d', 'Free pick-up and drop from Leh airport', 'plane', 1),
    ('leh-nubra-turtuk-pangong-6n-7d', 'Accommodation (2 adults sharing 1 room) with meals (breakfast and dinner)', 'bed-double', 2),
    ('leh-nubra-turtuk-pangong-6n-7d', 'Separate car per the itinerary (NGT terms apply; A/C not used in hilly areas)', 'car', 3),
    ('leh-nubra-turtuk-pangong-6n-7d', 'Inner Line Permits for Leh Ladakh', 'ticket', 4),

    ('leh-bike-tour-6n-7d', 'Bike with helmets', 'bike', 1),
    ('leh-bike-tour-6n-7d', 'Accommodation with meals (breakfast and dinner)', 'bed-double', 2),
    ('leh-bike-tour-6n-7d', 'Transportation for pick-up/drop-off and sightseeing', 'car', 3),
    ('leh-bike-tour-6n-7d', 'Inner Line Permits', 'ticket', 4),

    ('leh-nubra-pangong-tsomoriri-7n-8d', 'Accommodation with meals (breakfast and dinner)', 'bed-double', 1),
    ('leh-nubra-pangong-tsomoriri-7n-8d', 'Transportation for pick-up/drop-off and sightseeing', 'car', 2),
    ('leh-nubra-pangong-tsomoriri-7n-8d', 'Separate car per the itinerary (NGT terms apply; A/C not used in hilly areas)', 'bus', 3),
    ('leh-nubra-pangong-tsomoriri-7n-8d', 'Inner Line Permits for Leh Ladakh', 'ticket', 4),

    ('leh-ladakh-8n-9d', 'Accommodation with meals (breakfast and dinner)', 'bed-double', 1),
    ('leh-ladakh-8n-9d', 'Transportation for pick-up/drop-off and sightseeing', 'car', 2),
    ('leh-ladakh-8n-9d', 'Separate car per the itinerary (NGT terms apply; A/C not used in hilly areas)', 'bus', 3),
    ('leh-ladakh-8n-9d', 'Inner Line Permits for Leh Ladakh', 'ticket', 4),

    ('leh-ladakh-bike-trip-umling-la', 'Accommodation with meals (breakfast and dinner)', 'bed-double', 1),
    ('leh-ladakh-bike-trip-umling-la', 'Transportation for pick-up/drop-off and sightseeing', 'car', 2),
    ('leh-ladakh-bike-trip-umling-la', 'Inner Line Permits', 'ticket', 3),
    ('leh-ladakh-bike-trip-umling-la', 'Bike, fuel and necessary spare parts', 'bike', 4),
    ('leh-ladakh-bike-trip-umling-la', 'Oxygen cylinder and first-aid kit', 'heart-pulse', 5),
    ('leh-ladakh-bike-trip-umling-la', 'Trained mechanic and ride captain', 'user-round', 6),
    ('leh-ladakh-bike-trip-umling-la', 'Arm guard, leg guard and riding gloves', 'shield', 7),

    ('leh-ladakh-via-road', 'Accommodation with meals (breakfast and dinner)', 'bed-double', 1),
    ('leh-ladakh-via-road', 'Transportation and sightseeing', 'car', 2),
    ('leh-ladakh-via-road', 'Inner Line Permits', 'ticket', 3),

    ('manali-to-leh-bike-tour-8n-9d', 'Accommodation with meals (breakfast and dinner)', 'bed-double', 1),
    ('manali-to-leh-bike-tour-8n-9d', 'Transportation and sightseeing', 'car', 2),
    ('manali-to-leh-bike-tour-8n-9d', 'Inner Line Permits', 'ticket', 3)
) AS v(slug, descr, icon, ord);

-- =====================================================================
-- 6. Exclusions — a common core for every package, plus riding gear for the
--    Leh Bike Trip.
-- =====================================================================
INSERT INTO package_excludes (package_id, description, icon, display_order)
SELECT tp.id, v.descr, v.icon, v.ord
FROM tour_packages tp
CROSS JOIN (VALUES
    ('Any extra service for places not included in the itinerary', 'circle-minus', 1),
    ('5% GST', 'receipt', 2),
    ('Boating and any entry charges', 'wallet', 3)
) AS v(descr, icon, ord)
WHERE tp.slug IN (
    'leh-tour-5n-6d', 'leh-bike-trip-5n-6d', 'leh-nubra-turtuk-pangong-6n-7d', 'leh-bike-tour-6n-7d',
    'leh-nubra-pangong-tsomoriri-7n-8d', 'leh-ladakh-8n-9d', 'leh-ladakh-bike-trip-umling-la',
    'leh-ladakh-via-road', 'manali-to-leh-bike-tour-8n-9d');

INSERT INTO package_excludes (package_id, description, icon, display_order)
SELECT tp.id, 'Riding gear', 'shield', 4
FROM tour_packages tp WHERE tp.slug = 'leh-bike-trip-5n-6d';
