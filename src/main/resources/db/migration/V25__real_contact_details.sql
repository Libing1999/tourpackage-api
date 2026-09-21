-- The operator's real contact details, replacing the placeholders set in V21.
-- The contact page's map is geocoded from contact_address, so updating that
-- value moves the map to the Leh Main Bazar location.
--
-- Two phone numbers are provided: the calling number goes in contact_phone,
-- and the WhatsApp number in a new public contact_whatsapp key that the footer
-- and contact page render with a wa.me link.

UPDATE settings SET value = '100sky.ladakh@gmail.com' WHERE key = 'contact_email';
UPDATE settings SET value = '+91 94695 30962'          WHERE key = 'contact_phone';
UPDATE settings SET value = 'DB2 Complex, near SBI Bank, Main Bazar, Leh, Ladakh 194101' WHERE key = 'contact_address';

INSERT INTO settings (key, value, value_type, group_name, is_public) VALUES
    ('contact_whatsapp', '+91 84928 33393', 'STRING', 'CONTACT', TRUE)
ON CONFLICT (key) DO UPDATE
    SET value = EXCLUDED.value,
        value_type = EXCLUDED.value_type,
        group_name = EXCLUDED.group_name,
        is_public = EXCLUDED.is_public;
