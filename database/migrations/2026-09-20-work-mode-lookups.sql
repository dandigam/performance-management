INSERT INTO lookup_types (code, name, description, is_active)
SELECT 'WORK_MODE', 'Work Mode', 'Employee work mode options', TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM lookup_types WHERE UPPER(code) = 'WORK_MODE'
);

INSERT INTO lookup_values
    (lookup_type_id, code, name, description, display_order, is_active)
SELECT type.id, 'OFFSHORE', 'Offshore', 'Employee works offshore', 1, TRUE
FROM lookup_types type
WHERE UPPER(type.code) = 'WORK_MODE'
  AND NOT EXISTS (
      SELECT 1 FROM lookup_values value
      WHERE value.lookup_type_id = type.id AND UPPER(value.code) = 'OFFSHORE'
  );

INSERT INTO lookup_values
    (lookup_type_id, code, name, description, display_order, is_active)
SELECT type.id, 'ONSITE', 'Onsite', 'Employee works onsite', 2, TRUE
FROM lookup_types type
WHERE UPPER(type.code) = 'WORK_MODE'
  AND NOT EXISTS (
      SELECT 1 FROM lookup_values value
      WHERE value.lookup_type_id = type.id AND UPPER(value.code) = 'ONSITE'
  );
