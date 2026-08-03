-- Demo content so the public homepage has something real to fetch instead
-- of rendering empty states everywhere. This is seed/demo data, not
-- production content — a real deployment replaces it via the (future)
-- admin content-management screens, not by editing this file.
--
-- FK lookups use natural keys (iso2, slug, email) via subqueries rather
-- than hardcoded UUIDs, so this migration doesn't need to know IDs that
-- gen_random_uuid() hasn't generated yet.

-- ---------------------------------------------------------------------
-- Countries
-- ---------------------------------------------------------------------
INSERT INTO countries (name, iso2, iso3, phone_code, currency_code, flag_emoji) VALUES
    ('Thailand', 'TH', 'THA', '+66', 'THB', '🇹🇭'),
    ('Italy', 'IT', 'ITA', '+39', 'EUR', '🇮🇹'),
    ('France', 'FR', 'FRA', '+33', 'EUR', '🇫🇷'),
    ('United Arab Emirates', 'AE', 'ARE', '+971', 'AED', '🇦🇪'),
    ('Indonesia', 'ID', 'IDN', '+62', 'IDR', '🇮🇩');

-- ---------------------------------------------------------------------
-- Cities
-- ---------------------------------------------------------------------
INSERT INTO cities (country_id, name, slug, is_popular, image_url) VALUES
    ((SELECT id FROM countries WHERE iso2 = 'TH'), 'Bangkok', 'bangkok', TRUE,
     'https://images.unsplash.com/photo-1508009603885-50cf7c579365?w=1200&h=900&fit=crop&auto=format&q=80'),
    ((SELECT id FROM countries WHERE iso2 = 'TH'), 'Phuket', 'phuket', TRUE,
     'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=1200&h=900&fit=crop&auto=format&q=80'),
    ((SELECT id FROM countries WHERE iso2 = 'IT'), 'Rome', 'rome', TRUE,
     'https://images.unsplash.com/photo-1552832230-c0197dd311b5?w=1200&h=900&fit=crop&auto=format&q=80'),
    ((SELECT id FROM countries WHERE iso2 = 'IT'), 'Venice', 'venice', FALSE,
     'https://images.unsplash.com/photo-1523906834658-6e24ef2386f9?w=1200&h=900&fit=crop&auto=format&q=80'),
    ((SELECT id FROM countries WHERE iso2 = 'FR'), 'Paris', 'paris', TRUE,
     'https://images.unsplash.com/photo-1502602898657-3e91760cbb34?w=1200&h=900&fit=crop&auto=format&q=80'),
    ((SELECT id FROM countries WHERE iso2 = 'AE'), 'Dubai', 'dubai', TRUE,
     'https://images.unsplash.com/photo-1512453979798-5ea266f8880c?w=1200&h=900&fit=crop&auto=format&q=80'),
    ((SELECT id FROM countries WHERE iso2 = 'ID'), 'Bali', 'bali', TRUE,
     'https://images.unsplash.com/photo-1544644181-1484b3fdfc62?w=1200&h=900&fit=crop&auto=format&q=80'),
    ((SELECT id FROM countries WHERE iso2 = 'ID'), 'Yogyakarta', 'yogyakarta', FALSE,
     'https://images.unsplash.com/photo-1596402184320-417e7178b2cd?w=1200&h=900&fit=crop&auto=format&q=80');

-- ---------------------------------------------------------------------
-- Hotels (+ one cover image each)
-- ---------------------------------------------------------------------
INSERT INTO hotels (name, slug, short_description, description, star_rating, city_id, address_line1,
                     base_price, currency_code, rating_average, rating_count, is_featured, status, created_by) VALUES
    ('Riverside Grand Bangkok', 'riverside-grand-bangkok',
     'Riverside luxury in the heart of Bangkok.',
     'A five-star riverside retreat blending Thai hospitality with skyline views over the Chao Phraya.',
     5, (SELECT id FROM cities WHERE slug = 'bangkok'), '1 Charoen Krung Road',
     180.00, 'USD', 4.60, 812, TRUE, 'PUBLISHED', (SELECT id FROM admins WHERE email = 'admin@tourpackage.com')),
    ('Andaman Beach Resort', 'andaman-beach-resort',
     'Beachfront resort on Phuket''s west coast.',
     'Private beach access, infinity pools, and sunset views over the Andaman Sea.',
     5, (SELECT id FROM cities WHERE slug = 'phuket'), '88 Patong Beach Road',
     220.00, 'USD', 4.80, 1043, TRUE, 'PUBLISHED', (SELECT id FROM admins WHERE email = 'admin@tourpackage.com')),
    ('Colosseo Boutique Hotel', 'colosseo-boutique-hotel',
     'Steps from the Colosseum.',
     'A boutique stay in a restored 19th-century townhouse minutes from Rome''s ancient core.',
     4, (SELECT id FROM cities WHERE slug = 'rome'), 'Via Sacra 12',
     160.00, 'USD', 4.50, 634, FALSE, 'PUBLISHED', (SELECT id FROM admins WHERE email = 'admin@tourpackage.com')),
    ('Canal View Palazzo', 'canal-view-palazzo',
     'A restored palazzo on the Grand Canal.',
     'Venetian elegance with private water-taxi access and canal-facing suites.',
     5, (SELECT id FROM cities WHERE slug = 'venice'), 'Fondamenta Grande 45',
     260.00, 'USD', 4.70, 421, TRUE, 'PUBLISHED', (SELECT id FROM admins WHERE email = 'admin@tourpackage.com')),
    ('Le Jardin Parisien', 'le-jardin-parisien',
     'Classic Parisian elegance near the Champs-Élysées.',
     'A five-star maison with a private garden courtyard, a short walk from the Arc de Triomphe.',
     5, (SELECT id FROM cities WHERE slug = 'paris'), '24 Avenue Montaigne',
     310.00, 'USD', 4.90, 967, TRUE, 'PUBLISHED', (SELECT id FROM admins WHERE email = 'admin@tourpackage.com')),
    ('Burj Oasis Hotel', 'burj-oasis-hotel',
     'Skyline luxury in Downtown Dubai.',
     'Floor-to-ceiling views of the Burj Khalifa, an infinity pool, and a five-star spa.',
     5, (SELECT id FROM cities WHERE slug = 'dubai'), '1 Sheikh Mohammed Bin Rashid Blvd',
     340.00, 'USD', 4.80, 1287, TRUE, 'PUBLISHED', (SELECT id FROM admins WHERE email = 'admin@tourpackage.com')),
    ('Bali Serenity Villas', 'bali-serenity-villas',
     'Private pool villas above the Ubud rice terraces.',
     'Secluded villas with private plunge pools overlooking jungle and terraced rice paddies.',
     4, (SELECT id FROM cities WHERE slug = 'bali'), 'Jalan Raya Ubud 21',
     150.00, 'USD', 4.60, 553, FALSE, 'PUBLISHED', (SELECT id FROM admins WHERE email = 'admin@tourpackage.com')),
    ('Yogyakarta Heritage Inn', 'yogyakarta-heritage-inn',
     'A courtyard inn near the Sultan''s Palace.',
     'Traditional Javanese architecture and gamelan evenings, walking distance from Kraton palace.',
     3, (SELECT id FROM cities WHERE slug = 'yogyakarta'), 'Jalan Malioboro 5',
     70.00, 'USD', 4.30, 208, FALSE, 'PUBLISHED', (SELECT id FROM admins WHERE email = 'admin@tourpackage.com'));

INSERT INTO hotel_images (hotel_id, url, alt_text, is_cover) VALUES
    ((SELECT id FROM hotels WHERE slug = 'riverside-grand-bangkok'),
     'https://images.unsplash.com/photo-1571003123894-1f0594d2b5d9?w=1200&h=800&fit=crop&auto=format&q=80', 'Riverside Grand Bangkok exterior', TRUE),
    ((SELECT id FROM hotels WHERE slug = 'andaman-beach-resort'),
     'https://images.unsplash.com/photo-1566073771259-6a8506099945?w=1200&h=800&fit=crop&auto=format&q=80', 'Andaman Beach Resort pool', TRUE),
    ((SELECT id FROM hotels WHERE slug = 'colosseo-boutique-hotel'),
     'https://images.unsplash.com/photo-1571896349842-33c89424de2d?w=1200&h=800&fit=crop&auto=format&q=80', 'Colosseo Boutique Hotel lobby', TRUE),
    ((SELECT id FROM hotels WHERE slug = 'canal-view-palazzo'),
     'https://images.unsplash.com/photo-1564501049412-61c2a3083791?w=1200&h=800&fit=crop&auto=format&q=80', 'Canal View Palazzo facade', TRUE),
    ((SELECT id FROM hotels WHERE slug = 'le-jardin-parisien'),
     'https://images.unsplash.com/photo-1502602898657-3e91760cbb34?w=1200&h=800&fit=crop&auto=format&q=80', 'Le Jardin Parisien courtyard', TRUE),
    ((SELECT id FROM hotels WHERE slug = 'burj-oasis-hotel'),
     'https://images.unsplash.com/photo-1512453979798-5ea266f8880c?w=1200&h=800&fit=crop&auto=format&q=80', 'Burj Oasis Hotel skyline view', TRUE),
    ((SELECT id FROM hotels WHERE slug = 'bali-serenity-villas'),
     'https://images.unsplash.com/photo-1467987506553-8f3916508521?w=1200&h=800&fit=crop&auto=format&q=80', 'Bali Serenity Villas pool', TRUE),
    ((SELECT id FROM hotels WHERE slug = 'yogyakarta-heritage-inn'),
     'https://images.unsplash.com/photo-1596402184320-417e7178b2cd?w=1200&h=800&fit=crop&auto=format&q=80', 'Yogyakarta Heritage Inn courtyard', TRUE);

-- ---------------------------------------------------------------------
-- Tour packages (+ one cover image each). Four carry a discount_price,
-- which is exactly what the "Special Offers" section filters on.
-- ---------------------------------------------------------------------
INSERT INTO tour_packages (title, slug, summary, description, country_id, city_id, duration_days,
                            duration_nights, price, discount_price, rating_average, rating_count,
                            is_featured, status, created_by) VALUES
    ('Bangkok City Explorer', 'bangkok-city-explorer',
     'Temples, street food, and river cruises through the Thai capital.',
     'Four days through Bangkok''s grand palaces, floating markets, and rooftop skylines.',
     (SELECT id FROM countries WHERE iso2 = 'TH'), (SELECT id FROM cities WHERE slug = 'bangkok'),
     4, 3, 499.00, NULL, 4.50, 312, TRUE, 'PUBLISHED', (SELECT id FROM admins WHERE email = 'admin@tourpackage.com')),
    ('Phuket Island Paradise', 'phuket-island-paradise',
     'Beaches, snorkeling, and island-hopping across the Andaman Sea.',
     'Six days of beach resorts, speedboat island tours, and sunset catamaran cruises.',
     (SELECT id FROM countries WHERE iso2 = 'TH'), (SELECT id FROM cities WHERE slug = 'phuket'),
     6, 5, 899.00, 749.00, 4.70, 501, TRUE, 'PUBLISHED', (SELECT id FROM admins WHERE email = 'admin@tourpackage.com')),
    ('Roman Holiday Classic', 'roman-holiday-classic',
     'The Colosseum, the Vatican, and Rome''s cobbled backstreets.',
     'Five days tracing ancient Rome, Vatican City, and the trattorias of Trastevere.',
     (SELECT id FROM countries WHERE iso2 = 'IT'), (SELECT id FROM cities WHERE slug = 'rome'),
     5, 4, 999.00, NULL, 4.60, 278, FALSE, 'PUBLISHED', (SELECT id FROM admins WHERE email = 'admin@tourpackage.com')),
    ('Venice Romance Getaway', 'venice-romance-getaway',
     'Gondolas, canals, and candlelit dinners on the water.',
     'Three days gliding through Venice''s canals with a private gondola tour included.',
     (SELECT id FROM countries WHERE iso2 = 'IT'), (SELECT id FROM cities WHERE slug = 'venice'),
     3, 2, 649.00, 549.00, 4.80, 189, FALSE, 'PUBLISHED', (SELECT id FROM admins WHERE email = 'admin@tourpackage.com')),
    ('Paris City of Lights', 'paris-city-of-lights',
     'The Eiffel Tower, the Louvre, and Seine river cruises.',
     'Five days in Paris covering the Louvre, Montmartre, and a private Eiffel Tower dinner.',
     (SELECT id FROM countries WHERE iso2 = 'FR'), (SELECT id FROM cities WHERE slug = 'paris'),
     5, 4, 1199.00, NULL, 4.90, 645, TRUE, 'PUBLISHED', (SELECT id FROM admins WHERE email = 'admin@tourpackage.com')),
    ('Dubai Luxury Escape', 'dubai-luxury-escape',
     'Desert safaris, the Burj Khalifa, and five-star skylines.',
     'Four days combining desert dune safaris with Burj Khalifa dining and Palm Jumeirah beaches.',
     (SELECT id FROM countries WHERE iso2 = 'AE'), (SELECT id FROM cities WHERE slug = 'dubai'),
     4, 3, 1399.00, 1099.00, 4.80, 734, TRUE, 'PUBLISHED', (SELECT id FROM admins WHERE email = 'admin@tourpackage.com')),
    ('Bali Tropical Retreat', 'bali-tropical-retreat',
     'Rice terraces, temples, and private pool villas.',
     'Seven days across Ubud''s rice terraces, Uluwatu''s cliffside temples, and Seminyak''s beaches.',
     (SELECT id FROM countries WHERE iso2 = 'ID'), (SELECT id FROM cities WHERE slug = 'bali'),
     7, 6, 1099.00, NULL, 4.70, 456, TRUE, 'PUBLISHED', (SELECT id FROM admins WHERE email = 'admin@tourpackage.com')),
    ('Yogyakarta Cultural Trail', 'yogyakarta-cultural-trail',
     'Borobudur sunrise, royal palaces, and batik workshops.',
     'Four days exploring Borobudur at sunrise, the Sultan''s Palace, and traditional batik villages.',
     (SELECT id FROM countries WHERE iso2 = 'ID'), (SELECT id FROM cities WHERE slug = 'yogyakarta'),
     4, 3, 549.00, 449.00, 4.40, 167, FALSE, 'PUBLISHED', (SELECT id FROM admins WHERE email = 'admin@tourpackage.com'));

INSERT INTO package_images (package_id, url, alt_text, is_cover) VALUES
    ((SELECT id FROM tour_packages WHERE slug = 'bangkok-city-explorer'),
     'https://images.unsplash.com/photo-1508009603885-50cf7c579365?w=1200&h=800&fit=crop&auto=format&q=80', 'Bangkok temple skyline', TRUE),
    ((SELECT id FROM tour_packages WHERE slug = 'phuket-island-paradise'),
     'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=1200&h=800&fit=crop&auto=format&q=80', 'Phuket island beach', TRUE),
    ((SELECT id FROM tour_packages WHERE slug = 'roman-holiday-classic'),
     'https://images.unsplash.com/photo-1552832230-c0197dd311b5?w=1200&h=800&fit=crop&auto=format&q=80', 'Roman Colosseum', TRUE),
    ((SELECT id FROM tour_packages WHERE slug = 'venice-romance-getaway'),
     'https://images.unsplash.com/photo-1523906834658-6e24ef2386f9?w=1200&h=800&fit=crop&auto=format&q=80', 'Venice canal gondola', TRUE),
    ((SELECT id FROM tour_packages WHERE slug = 'paris-city-of-lights'),
     'https://images.unsplash.com/photo-1502602898657-3e91760cbb34?w=1200&h=800&fit=crop&auto=format&q=80', 'Eiffel Tower Paris', TRUE),
    ((SELECT id FROM tour_packages WHERE slug = 'dubai-luxury-escape'),
     'https://images.unsplash.com/photo-1512453979798-5ea266f8880c?w=1200&h=800&fit=crop&auto=format&q=80', 'Dubai skyline', TRUE),
    ((SELECT id FROM tour_packages WHERE slug = 'bali-tropical-retreat'),
     'https://images.unsplash.com/photo-1544644181-1484b3fdfc62?w=1200&h=800&fit=crop&auto=format&q=80', 'Bali rice terraces', TRUE),
    ((SELECT id FROM tour_packages WHERE slug = 'yogyakarta-cultural-trail'),
     'https://images.unsplash.com/photo-1596402184320-417e7178b2cd?w=1200&h=800&fit=crop&auto=format&q=80', 'Borobudur temple sunrise', TRUE);

-- ---------------------------------------------------------------------
-- Testimonials
-- ---------------------------------------------------------------------
INSERT INTO testimonials (package_id, customer_name, customer_avatar_url, customer_country_id, rating, message, is_featured, is_active) VALUES
    ((SELECT id FROM tour_packages WHERE slug = 'phuket-island-paradise'), 'Sarah Mitchell',
     'https://images.unsplash.com/photo-1524250502761-1ac6f2e30d43?w=200&h=200&fit=crop&auto=format&q=80',
     (SELECT id FROM countries WHERE iso2 = 'FR'), 5,
     'The Phuket package exceeded every expectation — the island-hopping day alone was worth the trip. Our guide knew every hidden cove.',
     TRUE, TRUE),
    ((SELECT id FROM tour_packages WHERE slug = 'paris-city-of-lights'), 'James Anderson',
     'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=200&h=200&fit=crop&auto=format&q=80',
     (SELECT id FROM countries WHERE iso2 = 'AE'), 5,
     'Paris in five days felt effortless. The private Eiffel Tower dinner was the highlight of our anniversary trip.',
     TRUE, TRUE),
    ((SELECT id FROM tour_packages WHERE slug = 'dubai-luxury-escape'), 'Priya Sharma',
     'https://images.unsplash.com/photo-1531123897727-8f129e1688ce?w=200&h=200&fit=crop&auto=format&q=80',
     (SELECT id FROM countries WHERE iso2 = 'IT'), 4,
     'The desert safari at sunset was unforgettable, and the hotel upgrade they arranged made the whole trip feel five-star.',
     TRUE, TRUE),
    ((SELECT id FROM tour_packages WHERE slug = 'bali-tropical-retreat'), 'Marco Rossi',
     'https://images.unsplash.com/photo-1531891437562-4301cf35b7e4?w=200&h=200&fit=crop&auto=format&q=80',
     (SELECT id FROM countries WHERE iso2 = 'TH'), 5,
     'Bali''s rice terraces at sunrise, private pool villa, and the team''s attention to every detail — we''re already planning our next trip with them.',
     TRUE, TRUE),
    (NULL, 'Emily Chen',
     'https://images.unsplash.com/photo-1487412720507-e7ab37603c6f?w=200&h=200&fit=crop&auto=format&q=80',
     (SELECT id FROM countries WHERE iso2 = 'ID'), 5,
     'Booking was simple, support was fast whenever we had questions, and every hotel they picked was better than the photos.',
     TRUE, TRUE),
    ((SELECT id FROM tour_packages WHERE slug = 'roman-holiday-classic'), 'David Thompson',
     'https://images.unsplash.com/photo-1560250097-0b93528c311a?w=200&h=200&fit=crop&auto=format&q=80',
     (SELECT id FROM countries WHERE iso2 = 'AE'), 4,
     'Skip-the-line Vatican access saved us hours. Rome felt easy to navigate with their itinerary in hand.',
     TRUE, TRUE);

-- ---------------------------------------------------------------------
-- FAQs
-- ---------------------------------------------------------------------
INSERT INTO faqs (question, answer, category, display_order) VALUES
    ('How do I book a tour package?', 'Browse any package and click "Book Now" to start the reservation flow — you''ll choose your travel dates, number of travellers, and complete payment in one checkout.', 'BOOKING', 1),
    ('Can I customize a tour package?', 'Yes — contact our travel consultants via the inquiry form on any package page and we''ll tailor the itinerary, hotel tier, or duration to your group.', 'BOOKING', 2),
    ('What payment methods do you accept?', 'We accept all major credit and debit cards, PayPal, and bank transfer for larger group bookings.', 'PAYMENT', 1),
    ('Is my payment information secure?', 'Yes. Payments are processed through PCI-compliant providers — we never store your card details on our servers.', 'PAYMENT', 2),
    ('What is your cancellation policy?', 'Full refunds are available up to 14 days before departure. Cancellations within 14 days are subject to the fare rules of the specific package.', 'CANCELLATION', 1),
    ('Can I reschedule my trip?', 'Most packages allow one free reschedule up to 7 days before departure, subject to hotel and flight availability.', 'CANCELLATION', 2),
    ('Do I need travel insurance?', 'We strongly recommend it. Travel insurance isn''t included by default but can be added during checkout on any package.', 'GENERAL', 1),
    ('Do you offer group discounts?', 'Yes — groups of 8 or more travellers qualify for tiered discounts. Reach out via the contact form for a custom quote.', 'GENERAL', 2);

-- ---------------------------------------------------------------------
-- Banners (hero slider)
-- ---------------------------------------------------------------------
INSERT INTO banners (title, subtitle, image_url, link_url, button_label, display_order, is_active) VALUES
    ('Discover the World with TourPackage', 'Handpicked destinations, curated itineraries, unforgettable memories.',
     'https://images.unsplash.com/photo-1488646953014-85cb44e25828?w=1920&h=1080&fit=crop&auto=format&q=80',
     '/packages', 'Explore Packages', 1, TRUE),
    ('Escape to Paradise', 'Island getaways in Phuket and Bali, starting from $749.',
     'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=1920&h=1080&fit=crop&auto=format&q=80',
     '/offers', 'View Offers', 2, TRUE),
    ('Luxury, Redefined', 'Five-star hotels in Dubai, Paris, and Rome, at prices you''ll love.',
     'https://images.unsplash.com/photo-1512453979798-5ea266f8880c?w=1920&h=1080&fit=crop&auto=format&q=80',
     '/hotels', 'Browse Hotels', 3, TRUE),
    ('Your Next Adventure Awaits', 'From temple sunrises to canal-side dinners — plan it with us.',
     'https://images.unsplash.com/photo-1526772662000-3f88f10405ff?w=1920&h=1080&fit=crop&auto=format&q=80',
     '/destinations', 'See Destinations', 4, TRUE);

-- ---------------------------------------------------------------------
-- Blog posts
-- ---------------------------------------------------------------------
INSERT INTO blog_posts (title, slug, excerpt, content, cover_image_url, category, author_id, status, published_at, read_time_minutes) VALUES
    ('10 Hidden Gems in Bangkok Beyond the Grand Palace', '10-hidden-gems-bangkok-beyond-grand-palace',
     'Skip the tourist queues — here''s where locals actually eat, shop, and unwind in Bangkok.',
     'Full article content covering Bangkok''s lesser-known neighborhoods, night markets, and riverside cafes.',
     'https://images.unsplash.com/photo-1508009603885-50cf7c579365?w=1200&h=800&fit=crop&auto=format&q=80',
     'Destination Guide', (SELECT id FROM admins WHERE email = 'admin@tourpackage.com'), 'PUBLISHED', now() - interval '2 days', 6),
    ('A Foodie''s Guide to Rome''s Trastevere District', 'foodies-guide-rome-trastevere',
     'Cobblestone streets, family-run trattorias, and the best gelato you''ll find outside a tourist trap.',
     'Full article content covering Trastevere''s food scene, from carbonara to gelaterias.',
     'https://images.unsplash.com/photo-1552832230-c0197dd311b5?w=1200&h=800&fit=crop&auto=format&q=80',
     'Food & Culture', (SELECT id FROM admins WHERE email = 'admin@tourpackage.com'), 'PUBLISHED', now() - interval '5 days', 5),
    ('Packing Smart: What to Bring on a Desert Safari', 'packing-smart-desert-safari',
     'Sand, sun, and sudden temperature drops — here''s the packing list our Dubai guides swear by.',
     'Full article content with a practical desert-safari packing checklist.',
     'https://images.unsplash.com/photo-1512453979798-5ea266f8880c?w=1200&h=800&fit=crop&auto=format&q=80',
     'Travel Tips', (SELECT id FROM admins WHERE email = 'admin@tourpackage.com'), 'PUBLISHED', now() - interval '9 days', 4),
    ('Why Bali''s Rice Terraces Are Best Seen at Sunrise', 'bali-rice-terraces-sunrise',
     'The light, the crowds, the temperature — timing your Tegallalang visit right makes all the difference.',
     'Full article content on the best times and viewpoints for Bali''s rice terraces.',
     'https://images.unsplash.com/photo-1544644181-1484b3fdfc62?w=1200&h=800&fit=crop&auto=format&q=80',
     'Destination Guide', (SELECT id FROM admins WHERE email = 'admin@tourpackage.com'), 'PUBLISHED', now() - interval '13 days', 5),
    ('A First-Timer''s Guide to Venice''s Vaporetto System', 'first-timers-guide-venice-vaporetto',
     'Venice has no cars — here''s how to actually get around its canals without overpaying for water taxis.',
     'Full article content explaining Venice''s public water-bus network and ticket options.',
     'https://images.unsplash.com/photo-1523906834658-6e24ef2386f9?w=1200&h=800&fit=crop&auto=format&q=80',
     'Travel Tips', (SELECT id FROM admins WHERE email = 'admin@tourpackage.com'), 'PUBLISHED', now() - interval '18 days', 4),
    ('Paris on a Budget: Free (and Nearly Free) Things to Do', 'paris-on-a-budget-free-things-to-do',
     'The Louvre isn''t the only way to spend a day in Paris — here''s how to see the city without spending much at all.',
     'Full article content listing free museums, parks, and viewpoints across Paris.',
     'https://images.unsplash.com/photo-1502602898657-3e91760cbb34?w=1200&h=800&fit=crop&auto=format&q=80',
     'Travel Tips', (SELECT id FROM admins WHERE email = 'admin@tourpackage.com'), 'PUBLISHED', now() - interval '24 days', 7);

-- ---------------------------------------------------------------------
-- Public settings (footer contact info, social links, site identity)
-- ---------------------------------------------------------------------
INSERT INTO settings (key, value, value_type, group_name, is_public) VALUES
    ('site_name', 'TourPackage', 'STRING', 'GENERAL', TRUE),
    ('site_tagline', 'Your journey, perfectly planned.', 'STRING', 'GENERAL', TRUE),
    ('contact_email', 'hello@tourpackage.com', 'STRING', 'CONTACT', TRUE),
    ('contact_phone', '+1 (555) 010-2024', 'STRING', 'CONTACT', TRUE),
    ('contact_address', '128 Travel Plaza, Suite 400, San Francisco, CA', 'STRING', 'CONTACT', TRUE),
    ('social_facebook', 'https://facebook.com/tourpackage', 'STRING', 'SOCIAL', TRUE),
    ('social_instagram', 'https://instagram.com/tourpackage', 'STRING', 'SOCIAL', TRUE),
    ('social_twitter', 'https://twitter.com/tourpackage', 'STRING', 'SOCIAL', TRUE),
    ('social_youtube', 'https://youtube.com/@tourpackage', 'STRING', 'SOCIAL', TRUE),
    ('business_hours', 'Mon–Fri, 9:00 AM – 6:00 PM (PST)', 'STRING', 'CONTACT', TRUE);
