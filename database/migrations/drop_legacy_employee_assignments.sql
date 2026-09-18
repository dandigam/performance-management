-- Run against the intended application database after the current
-- sow_employee_assignments table contains the assignments to retain.
-- This permanently removes the obsolete table and its remaining data.
-- Keep foreign-key checks enabled: existing references to the old table
-- must be migrated to sow_employee_assignments before this script can succeed.
SET SESSION FOREIGN_KEY_CHECKS = 1;
DROP TABLE IF EXISTS employee_assignments;
