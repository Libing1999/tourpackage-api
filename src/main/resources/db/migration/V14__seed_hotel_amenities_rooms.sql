-- Demo amenities/room-types/rooms for the 8 hotels seeded in V13, so the
-- Hotel Module's filters, gallery, and rooms UI have real data to render
-- instead of empty states. Seed/demo data, not production content — see
-- the note at the top of V13.

-- ---------------------------------------------------------------------
-- Amenities
-- ---------------------------------------------------------------------
INSERT INTO amenities (name, slug, icon, category, display_order) VALUES
    ('Free WiFi', 'free-wifi', 'wifi', 'GENERAL', 1),
    ('Free Parking', 'free-parking', 'car', 'GENERAL', 2),
    ('24-Hour Front Desk', '24-hour-front-desk', 'clock', 'GENERAL', 3),
    ('Airport Shuttle', 'airport-shuttle', 'bus', 'GENERAL', 4),
    ('Air Conditioning', 'air-conditioning', 'snowflake', 'ROOM', 5),
    ('Restaurant', 'restaurant', 'utensils', 'DINING', 6),
    ('Bar / Lounge', 'bar-lounge', 'martini', 'DINING', 7),
    ('Swimming Pool', 'swimming-pool', 'waves', 'OUTDOOR', 8),
    ('Fitness Center', 'fitness-center', 'dumbbell', 'WELLNESS', 9),
    ('Spa & Wellness', 'spa-wellness', 'sparkles', 'WELLNESS', 10),
    ('Pet Friendly', 'pet-friendly', 'paw-print', 'FAMILY', 11),
    ('Wheelchair Accessible', 'wheelchair-accessible', 'accessibility', 'ACCESSIBILITY', 12);

-- ---------------------------------------------------------------------
-- Room types
-- ---------------------------------------------------------------------
INSERT INTO room_types (name, slug, description, max_occupancy, display_order) VALUES
    ('Standard Room', 'standard-room',
     'Comfortable room with all essential amenities.', 2, 1),
    ('Deluxe Room', 'deluxe-room',
     'Spacious room with upgraded furnishings and city or garden views.', 2, 2),
    ('Executive Suite', 'executive-suite',
     'Separate living area, premium amenities, and priority services.', 3, 3);

-- ---------------------------------------------------------------------
-- Hotel amenities — every hotel gets the core set; higher-tier and
-- star-rated hotels layer on extras; a couple of resort-style stays get
-- the family/pet-friendly tag.
-- ---------------------------------------------------------------------
INSERT INTO hotel_amenities (hotel_id, amenity_id)
SELECT h.id, a.id FROM hotels h
JOIN amenities a ON a.slug IN ('free-wifi', 'air-conditioning', '24-hour-front-desk', 'free-parking', 'wheelchair-accessible');

INSERT INTO hotel_amenities (hotel_id, amenity_id)
SELECT h.id, a.id FROM hotels h
JOIN amenities a ON a.slug IN ('fitness-center', 'restaurant', 'airport-shuttle')
WHERE h.star_rating >= 4;

INSERT INTO hotel_amenities (hotel_id, amenity_id)
SELECT h.id, a.id FROM hotels h
JOIN amenities a ON a.slug IN ('swimming-pool', 'spa-wellness', 'bar-lounge')
WHERE h.star_rating = 5;

INSERT INTO hotel_amenities (hotel_id, amenity_id)
SELECT h.id, a.id FROM hotels h
JOIN amenities a ON a.slug = 'pet-friendly'
WHERE h.slug IN ('bali-serenity-villas', 'yogyakarta-heritage-inn');

-- ---------------------------------------------------------------------
-- Hotel rooms — one row per hotel per room type, priced as a multiple of
-- the hotel's base_price so cheaper and pricier hotels stay proportional.
-- ---------------------------------------------------------------------
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
        WHEN 'standard-room' THEN 28
        WHEN 'deluxe-room' THEN 38
        WHEN 'executive-suite' THEN 55
    END,
    ROUND(h.base_price * CASE rt.slug
        WHEN 'standard-room' THEN 1.0
        WHEN 'deluxe-room' THEN 1.35
        WHEN 'executive-suite' THEN 2.1
    END, 2),
    h.currency_code,
    CASE rt.slug
        WHEN 'standard-room' THEN 10
        WHEN 'deluxe-room' THEN 8
        WHEN 'executive-suite' THEN 3
    END,
    TRUE
FROM hotels h
CROSS JOIN room_types rt;
