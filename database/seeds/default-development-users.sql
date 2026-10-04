-- Run manually against the development database after system roles exist.
-- Default password: admin123 (BCrypt cost 12). Existing accounts are preserved.
-- These are role accounts with FULL portal access and no fabricated employee record.
START TRANSACTION;
INSERT INTO users
    (username, password, role_id, status, portal_access, session_version, created_on, updated_on)
SELECT seed.username, seed.password, role.id, 'ACTIVE', 'FULL', 0,
       CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
FROM (
    SELECT 'admin@rit.com' AS username, 'ADMIN' AS role_code,
           '$2a$12$5jdDk4h/IaBnfBj.taOmKOhK0kjYgLhyUpyfIy8u13J7Mh5jI/ocm' AS password
    UNION ALL
    SELECT 'hr@rit.com', 'HR',
           '$2a$12$04LStdLY2tiw3LGD.GiwG./5gjdDPD7A1XTzB5KuewCb3nKX2vpVi'
    UNION ALL
    SELECT 'fin@rit.com', 'FINANCE',
           '$2a$12$e0NmVggdUikQFc9qFTNuJuFFCyLGEoP18tkcL.hMMnqIie7aZkRvK'
) seed
JOIN lookup_types role_type ON UPPER(role_type.code) = 'SYSTEM_ROLE'
JOIN lookup_values role ON role.lookup_type_id = role_type.id AND UPPER(role.code) = seed.role_code
WHERE NOT EXISTS (SELECT 1 FROM users existing WHERE LOWER(existing.username) = seed.username);
COMMIT;
