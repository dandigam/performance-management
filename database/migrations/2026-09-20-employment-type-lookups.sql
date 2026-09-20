INSERT INTO lookup_types (code, name, description, is_active)
SELECT 'EMPLOYMENT_TYPE', 'Employment Type', 'Employee employment type options', TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM lookup_types WHERE UPPER(code) = 'EMPLOYMENT_TYPE'
);

INSERT INTO lookup_values
    (lookup_type_id, code, name, description, display_order, is_active)
SELECT type.id, 'FULL_TIME', 'Full Time', 'Full-time employee', 1, TRUE
FROM lookup_types type
WHERE UPPER(type.code) = 'EMPLOYMENT_TYPE'
  AND NOT EXISTS (
      SELECT 1 FROM lookup_values value
      WHERE value.lookup_type_id = type.id AND UPPER(value.code) = 'FULL_TIME'
  );

INSERT INTO lookup_values
    (lookup_type_id, code, name, description, display_order, is_active)
SELECT type.id, 'CONTRACT', 'Contract', 'Contract employee', 2, TRUE
FROM lookup_types type
WHERE UPPER(type.code) = 'EMPLOYMENT_TYPE'
  AND NOT EXISTS (
      SELECT 1 FROM lookup_values value
      WHERE value.lookup_type_id = type.id AND UPPER(value.code) = 'CONTRACT'
  );
