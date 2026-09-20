INSERT INTO lookup_types (code, name, description, is_active)
SELECT 'WORK_LOCATION', 'Work Location', 'Employee work location options', TRUE
WHERE NOT EXISTS (SELECT 1 FROM lookup_types WHERE UPPER(code) = 'WORK_LOCATION');

INSERT INTO lookup_values (lookup_type_id, code, name, description, display_order, is_active)
SELECT type.id, 'REMOTE', 'Remote', 'Employee works remotely', 1, TRUE
FROM lookup_types type
WHERE UPPER(type.code) = 'WORK_LOCATION'
  AND NOT EXISTS (SELECT 1 FROM lookup_values value
                  WHERE value.lookup_type_id = type.id AND UPPER(value.code) = 'REMOTE');

INSERT INTO lookup_values (lookup_type_id, code, name, description, display_order, is_active)
SELECT type.id, 'OFFICE', 'Office', 'Employee works from the office', 2, TRUE
FROM lookup_types type
WHERE UPPER(type.code) = 'WORK_LOCATION'
  AND NOT EXISTS (SELECT 1 FROM lookup_values value
                  WHERE value.lookup_type_id = type.id AND UPPER(value.code) = 'OFFICE');

INSERT INTO lookup_values (lookup_type_id, code, name, description, display_order, is_active)
SELECT type.id, 'HYBRID', 'Hybrid', 'Employee works from the office and remotely', 3, TRUE
FROM lookup_types type
WHERE UPPER(type.code) = 'WORK_LOCATION'
  AND NOT EXISTS (SELECT 1 FROM lookup_values value
                  WHERE value.lookup_type_id = type.id AND UPPER(value.code) = 'HYBRID');

UPDATE employees SET work_location = 'OFFICE' WHERE UPPER(work_location) = 'ONSITE';

UPDATE lookup_values value
JOIN lookup_types type ON type.id = value.lookup_type_id
SET value.is_active = FALSE
WHERE UPPER(type.code) = 'WORK_LOCATION' AND UPPER(value.code) = 'ONSITE';
