-- Bootstrap SUPER_ADMIN so there is at least one account able to log in
-- and create further admins. Pre-verified (email_verified_at set) since
-- there is no inbox to click a verification link from at seed time.
--
-- Credentials: admin@tourpackage.com / ChangeMe123!
-- Change this password immediately after first login in any real
-- environment — it is public (this file is committed to source control).
INSERT INTO admins (full_name, email, password_hash, role, is_active, email_verified_at)
VALUES (
    'Super Admin',
    'admin@tourpackage.com',
    '$2a$10$UFje.4WU0rDg9zrwntFBv.yZ/UUeGJV8xpmXNjiCzDLiV48QXEXfO',
    'SUPER_ADMIN',
    TRUE,
    now()
)
ON CONFLICT (email) DO NOTHING;
