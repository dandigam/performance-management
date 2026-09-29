-- MySQL 8. Run against the selected application database with one migration runner.
-- Repeatable: inserts missing codes only; preserves existing IDs, names and active flags.
-- No recipient addresses or environment-specific IDs are seeded.
START TRANSACTION;

INSERT INTO lookup_types (code, name, description, is_active)
SELECT 'NOTIFICATION_CATEGORY', 'Notification Category', 'Business notification subscription categories', TRUE
WHERE NOT EXISTS (SELECT 1 FROM lookup_types WHERE UPPER(code) = 'NOTIFICATION_CATEGORY');

INSERT INTO lookup_values (lookup_type_id, code, name, description, display_order, is_active)
SELECT t.id, 'ALL_NOTIFICATIONS', 'All Notifications', 'All Notifications notification subscriptions', 1, TRUE
FROM lookup_types t
WHERE UPPER(t.code) = 'NOTIFICATION_CATEGORY'
  AND NOT EXISTS (SELECT 1 FROM lookup_values v WHERE v.lookup_type_id = t.id AND UPPER(v.code) = 'ALL_NOTIFICATIONS');

INSERT INTO lookup_values (lookup_type_id, code, name, description, display_order, is_active)
SELECT t.id, 'ONBOARDING', 'Onboarding', 'Onboarding notification subscriptions', 2, TRUE
FROM lookup_types t
WHERE UPPER(t.code) = 'NOTIFICATION_CATEGORY'
  AND NOT EXISTS (SELECT 1 FROM lookup_values v WHERE v.lookup_type_id = t.id AND UPPER(v.code) = 'ONBOARDING');

INSERT INTO lookup_values (lookup_type_id, code, name, description, display_order, is_active)
SELECT t.id, 'LEAVE', 'Leave', 'Leave notification subscriptions', 3, TRUE
FROM lookup_types t
WHERE UPPER(t.code) = 'NOTIFICATION_CATEGORY'
  AND NOT EXISTS (SELECT 1 FROM lookup_values v WHERE v.lookup_type_id = t.id AND UPPER(v.code) = 'LEAVE');

INSERT INTO lookup_values (lookup_type_id, code, name, description, display_order, is_active)
SELECT t.id, 'TIMESHEET', 'Timesheet', 'Timesheet notification subscriptions', 4, TRUE
FROM lookup_types t
WHERE UPPER(t.code) = 'NOTIFICATION_CATEGORY'
  AND NOT EXISTS (SELECT 1 FROM lookup_values v WHERE v.lookup_type_id = t.id AND UPPER(v.code) = 'TIMESHEET');

INSERT INTO lookup_values (lookup_type_id, code, name, description, display_order, is_active)
SELECT t.id, 'PERFORMANCE_REVIEW', 'Performance Review', 'Performance Review notification subscriptions', 5, TRUE
FROM lookup_types t
WHERE UPPER(t.code) = 'NOTIFICATION_CATEGORY'
  AND NOT EXISTS (SELECT 1 FROM lookup_values v WHERE v.lookup_type_id = t.id AND UPPER(v.code) = 'PERFORMANCE_REVIEW');

INSERT INTO lookup_values (lookup_type_id, code, name, description, display_order, is_active)
SELECT t.id, 'SOW', 'Statement of Work', 'Statement of Work notification subscriptions', 6, TRUE
FROM lookup_types t
WHERE UPPER(t.code) = 'NOTIFICATION_CATEGORY'
  AND NOT EXISTS (SELECT 1 FROM lookup_values v WHERE v.lookup_type_id = t.id AND UPPER(v.code) = 'SOW');

INSERT INTO lookup_values (lookup_type_id, code, name, description, display_order, is_active)
SELECT t.id, 'INVOICE', 'Invoice and Payments', 'Invoice and Payments notification subscriptions', 7, TRUE
FROM lookup_types t
WHERE UPPER(t.code) = 'NOTIFICATION_CATEGORY'
  AND NOT EXISTS (SELECT 1 FROM lookup_values v WHERE v.lookup_type_id = t.id AND UPPER(v.code) = 'INVOICE');

INSERT INTO lookup_values (lookup_type_id, code, name, description, display_order, is_active)
SELECT t.id, 'RESOURCE_ALLOCATION', 'Resource Allocation', 'Resource Allocation notification subscriptions', 8, TRUE
FROM lookup_types t
WHERE UPPER(t.code) = 'NOTIFICATION_CATEGORY'
  AND NOT EXISTS (SELECT 1 FROM lookup_values v WHERE v.lookup_type_id = t.id AND UPPER(v.code) = 'RESOURCE_ALLOCATION');

COMMIT;

-- Verify all eight codes and review any existing disabled values.
SELECT t.code AS lookup_type, t.is_active AS type_active, v.id, v.code, v.name, v.is_active
FROM lookup_types t JOIN lookup_values v ON v.lookup_type_id = t.id
WHERE UPPER(t.code) = 'NOTIFICATION_CATEGORY'
ORDER BY v.display_order, v.id;

