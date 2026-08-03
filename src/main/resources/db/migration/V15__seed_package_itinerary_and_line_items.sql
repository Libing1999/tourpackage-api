-- Demo itinerary days and include/exclude line items for the 8 tour packages
-- seeded in V13, so the Tour Package Module's detail page has real content to
-- render instead of empty sections. Seed/demo data — see the note in V13.

-- ---------------------------------------------------------------------
-- Itinerary — one row per day for each package's full duration. Day 1 is
-- always arrival and the last day is always departure; the days in between
-- are generic sightseeing, since a hand-written narrative per package would
-- be 40+ rows of prose that a real deployment replaces anyway.
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
            'Arrive at ' || c.name || ', meet your guide, and transfer to the hotel. The rest of the day is free to settle in.'
        WHEN d.day_number = tp.duration_days THEN
            'Breakfast at the hotel, then transfer to the airport for your onward journey.'
        ELSE
            'A full day of guided sightseeing around ' || c.name || ', with time set aside for lunch and independent exploration.'
    END,
    tp.city_id,
    CASE
        WHEN d.day_number = 1 THEN 'Dinner'
        WHEN d.day_number = tp.duration_days THEN 'Breakfast'
        ELSE 'Breakfast, Lunch'
    END,
    CASE WHEN d.day_number = tp.duration_days THEN NULL ELSE c.name || ' Hotel' END
FROM tour_packages tp
JOIN cities c ON c.id = tp.city_id
CROSS JOIN LATERAL generate_series(1, tp.duration_days) AS d(day_number);


-- ---------------------------------------------------------------------
-- What's included — a common core for every package, plus airport transfers
-- and daily breakfast, which are near-universal for this kind of tour.
-- ---------------------------------------------------------------------
INSERT INTO package_includes (package_id, description, icon, display_order)
SELECT tp.id, v.description, v.icon, v.display_order
FROM tour_packages tp
CROSS JOIN (VALUES
    ('Accommodation for the full duration of the tour', 'bed-double', 1),
    ('Daily breakfast at the hotel', 'utensils', 2),
    ('Return airport transfers', 'car', 3),
    ('Professional English-speaking guide', 'user-round', 4),
    ('All sightseeing and entrance fees per the itinerary', 'ticket', 5),
    ('All applicable taxes and service charges', 'receipt', 6)
) AS v(description, icon, display_order);

-- Longer tours (5+ days) also cover internal transport between sights.
INSERT INTO package_includes (package_id, description, icon, display_order)
SELECT tp.id, 'Air-conditioned transport throughout the tour', 'bus', 7
FROM tour_packages tp
WHERE tp.duration_days >= 5;


-- ---------------------------------------------------------------------
-- What's excluded
-- ---------------------------------------------------------------------
INSERT INTO package_excludes (package_id, description, icon, display_order)
SELECT tp.id, v.description, v.icon, v.display_order
FROM tour_packages tp
CROSS JOIN (VALUES
    ('International flights to and from the destination', 'plane', 1),
    ('Travel insurance', 'shield', 2),
    ('Visa fees, where applicable', 'stamp', 3),
    ('Lunches and dinners not listed in the itinerary', 'utensils-crossed', 4),
    ('Personal expenses, minibar, and laundry', 'wallet', 5),
    ('Tips and gratuities for guides and drivers', 'hand-coins', 6)
) AS v(description, icon, display_order);
