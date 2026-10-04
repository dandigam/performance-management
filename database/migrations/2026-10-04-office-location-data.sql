-- Addresses transcribed from the supplied company contact image.
-- Apply after 2026-10-03-office-settings.sql. Safe to rerun for these addresses.
START TRANSACTION;

INSERT IGNORE INTO company_settings (id, portal_name, created_on, updated_on)
VALUES (1, 'RailInfo Tech', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6));
SELECT id FROM company_settings WHERE id = 1 FOR UPDATE;

INSERT INTO office_locations
  (office_name, address_line1, address_line2, city, state_region, postal_code,
   country_code, time_zone, phone, email, active, created_on, updated_on)
SELECT seed.office_name, seed.address_line1, seed.address_line2, seed.city,
       seed.state_region, seed.postal_code, seed.country_code, seed.time_zone,
       seed.phone, 'info@railinfotech.com', TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
FROM (
  SELECT 'Richardson Head Office' AS office_name,
         '300 N Coit Road' AS address_line1, 'Suite 340' AS address_line2,
         'Richardson' AS city, 'Texas' AS state_region, '75080' AS postal_code,
         'US' AS country_code, 'America/Chicago' AS time_zone, '+1 904-767-1685' AS phone
  UNION ALL
  SELECT 'Jacksonville Branch Office', '550 Water St', '8th Floor',
         'Jacksonville', 'Florida', '32202', 'US', 'America/New_York', '+1 904-767-1685'
  UNION ALL
  SELECT 'Nanakramguda Branch Office', '9th Floor, Survey No.142, Vamsiram Suvarna Durga Tech Park',
         'Financial District, Nanakramguda', 'Hyderabad', 'Telangana', '500008',
         'IN', 'Asia/Kolkata', NULL
  UNION ALL
  SELECT 'Narapally Branch Office', 'H.No.4-1/2/W, 2nd Floor, Divya Nagar Road',
         'Narapally Village, Ghatkesar Mandal, Medchal Malkajgiri District',
         'Hyderabad', 'Telangana', '500088', 'IN', 'Asia/Kolkata', NULL
) AS seed
WHERE NOT EXISTS (
  SELECT 1 FROM office_locations existing
  WHERE existing.country_code = seed.country_code
    AND existing.city = seed.city
    AND existing.address_line1 = seed.address_line1
    AND existing.address_line2 <=> seed.address_line2
);

UPDATE company_settings
SET main_office_id = (
      SELECT id FROM office_locations
      WHERE office_name = 'Richardson Head Office' AND country_code = 'US' AND active = TRUE
      ORDER BY id LIMIT 1
    ),
    updated_on = CURRENT_TIMESTAMP(6)
WHERE id = 1 AND main_office_id IS NULL;

COMMIT;
