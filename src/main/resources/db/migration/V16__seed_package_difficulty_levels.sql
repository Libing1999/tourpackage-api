-- V13 never set difficulty_level explicitly, so all 8 demo packages fell to
-- the column default ('EASY') — which left three of the four options on the
-- listing page's difficulty filter permanently returning zero results. These
-- assignments give the filter something to actually filter, based on how
-- much walking/hiking each itinerary realistically involves.
--
-- EXTREME is deliberately left with no demo data: none of the seeded
-- packages are expedition-grade, and inventing one to fill out the enum
-- would misrepresent the content.

UPDATE tour_packages SET difficulty_level = 'EASY'
WHERE slug IN ('dubai-luxury-escape', 'venice-romance-getaway', 'bangkok-city-explorer');

UPDATE tour_packages SET difficulty_level = 'MODERATE'
WHERE slug IN ('paris-city-of-lights', 'roman-holiday-classic', 'bali-tropical-retreat');

UPDATE tour_packages SET difficulty_level = 'CHALLENGING'
WHERE slug IN ('yogyakarta-cultural-trail', 'phuket-island-paradise');
