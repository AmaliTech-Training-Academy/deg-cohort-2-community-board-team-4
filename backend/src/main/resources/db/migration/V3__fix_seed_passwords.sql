UPDATE users
SET password = '$2b$10$J./KngsZaewgPlUKts/tE.IUPHxeW3b2PgPFg6BlT7FoBRAJ6cRgG',
    updated_at = NOW()
WHERE email = 'admin@amalitech.com';

UPDATE users
SET password = '$2b$10$Fmxs/wd/psAKCLtmxxeDW.gNEeANTefNsT1WvuOLYVCj0BJLXvfoi',
    updated_at = NOW()
WHERE email = 'user@amalitech.com';
