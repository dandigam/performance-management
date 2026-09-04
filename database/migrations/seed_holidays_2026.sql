INSERT INTO holidays
    (holiday_name, holiday_date, location_type, description, is_active, created_on, updated_on)
SELECT seed.holiday_name, seed.holiday_date, seed.location_type,
       seed.description, 1, NOW(6), NOW(6)
FROM (
    SELECT 'New Year''s Day' holiday_name, DATE '2026-01-01' holiday_date, 'ONSITE' location_type, '2026 U.S. holiday calendar' description
    UNION ALL SELECT 'Martin Luther King Jr. Day', DATE '2026-01-19', 'ONSITE', '2026 U.S. holiday calendar'
    UNION ALL SELECT 'Presidents'' Day', DATE '2026-02-16', 'ONSITE', '2026 U.S. holiday calendar'
    UNION ALL SELECT 'Memorial Day', DATE '2026-05-25', 'ONSITE', '2026 U.S. holiday calendar'
    UNION ALL SELECT 'Juneteenth National Independence Day', DATE '2026-06-19', 'ONSITE', '2026 U.S. holiday calendar'
    UNION ALL SELECT 'Independence Day (Observed)', DATE '2026-07-03', 'ONSITE', '2026 U.S. holiday calendar'
    UNION ALL SELECT 'Labor Day', DATE '2026-09-07', 'ONSITE', '2026 U.S. holiday calendar'
    UNION ALL SELECT 'Columbus Day', DATE '2026-10-12', 'ONSITE', '2026 U.S. holiday calendar'
    UNION ALL SELECT 'Veterans Day', DATE '2026-11-11', 'ONSITE', '2026 U.S. holiday calendar'
    UNION ALL SELECT 'Thanksgiving Day', DATE '2026-11-26', 'ONSITE', '2026 U.S. holiday calendar'
    UNION ALL SELECT 'Christmas Day', DATE '2026-12-25', 'ONSITE', '2026 U.S. holiday calendar'
    UNION ALL SELECT 'Republic Day', DATE '2026-01-26', 'OFFSHORE', '2026 India holiday calendar'
    UNION ALL SELECT 'Holi', DATE '2026-03-04', 'OFFSHORE', '2026 India holiday calendar'
    UNION ALL SELECT 'Good Friday', DATE '2026-04-03', 'OFFSHORE', '2026 India holiday calendar'
    UNION ALL SELECT 'Independence Day', DATE '2026-08-15', 'OFFSHORE', '2026 India holiday calendar'
    UNION ALL SELECT 'Gandhi Jayanti', DATE '2026-10-02', 'OFFSHORE', '2026 India holiday calendar'
    UNION ALL SELECT 'Dussehra', DATE '2026-10-20', 'OFFSHORE', '2026 India holiday calendar'
    UNION ALL SELECT 'Diwali', DATE '2026-11-08', 'OFFSHORE', '2026 India holiday calendar'
    UNION ALL SELECT 'Christmas Day', DATE '2026-12-25', 'OFFSHORE', '2026 India holiday calendar'
) seed
LEFT JOIN holidays existing
  ON existing.location_type = seed.location_type
 AND existing.holiday_date = seed.holiday_date
WHERE existing.id IS NULL;
