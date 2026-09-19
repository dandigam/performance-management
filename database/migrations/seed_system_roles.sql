INSERT INTO lookup_types (code, name, description, is_active, created_on, updated_on)
SELECT 'SYSTEM_ROLE', 'System Role', 'Application and employee system roles', 1, NOW(6), NOW(6)
WHERE NOT EXISTS (
    SELECT 1
    FROM lookup_types
    WHERE UPPER(code) = 'SYSTEM_ROLE'
);

INSERT INTO lookup_values
    (lookup_type_id, code, name, description, display_order, is_active,
     created_on, updated_on)
SELECT role_type.id, seed.code, seed.name, seed.description, seed.display_order,
       1, NOW(6), NOW(6)
FROM lookup_types role_type
JOIN (
    SELECT 'ADMIN' code, 'Admin' name,
           'Full system administration access' description, 1 display_order
    UNION ALL SELECT 'HR', 'HR',
           'Employee and performance management access', 2
    UNION ALL SELECT 'FINANCE', 'Finance',
           'Finance, vendor, and banking access', 3
    UNION ALL SELECT 'MANAGER', 'Manager',
           'Manager-level employee and review responsibilities', 4
    UNION ALL SELECT 'TEAMLEAD', 'Team Lead',
           'Team lead employee and review responsibilities', 5
    UNION ALL SELECT 'EMPLOYEE', 'Employee',
           'Standard employee and self-review access', 6
) seed
WHERE UPPER(role_type.code) = 'SYSTEM_ROLE'
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description),
    display_order = VALUES(display_order),
    is_active = VALUES(is_active);