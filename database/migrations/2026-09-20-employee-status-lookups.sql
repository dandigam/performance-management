INSERT INTO lookup_types (code, name, description, is_active)
SELECT 'EMPLOYEE_STATUS', 'Employee Status', 'Employee status options', TRUE
WHERE NOT EXISTS (SELECT 1 FROM lookup_types WHERE UPPER(code) = 'EMPLOYEE_STATUS');

INSERT INTO lookup_values (lookup_type_id, code, name, description, display_order, is_active)
SELECT type.id, 'ACTIVE', 'Active', 'Employee is active', 1, TRUE
FROM lookup_types type
WHERE UPPER(type.code) = 'EMPLOYEE_STATUS'
  AND NOT EXISTS (SELECT 1 FROM lookup_values value
                  WHERE value.lookup_type_id = type.id AND UPPER(value.code) = 'ACTIVE');

INSERT INTO lookup_values (lookup_type_id, code, name, description, display_order, is_active)
SELECT type.id, 'INACTIVE', 'Inactive', 'Employee is inactive', 2, TRUE
FROM lookup_types type
WHERE UPPER(type.code) = 'EMPLOYEE_STATUS'
  AND NOT EXISTS (SELECT 1 FROM lookup_values value
                  WHERE value.lookup_type_id = type.id AND UPPER(value.code) = 'INACTIVE');
