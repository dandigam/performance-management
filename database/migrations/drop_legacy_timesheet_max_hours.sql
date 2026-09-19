-- The entity now persists default_hours_per_day.
-- Preserve legacy values, then remove the obsolete required column.
SET @drop_legacy_max_hours_sql = IF(
    EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'timesheet_employee_projects'
          AND column_name = 'default_hours_per_day'
    )
    AND EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'timesheet_employee_projects'
          AND column_name = 'max_hours_per_day'
    ),
    'UPDATE timesheet_employee_projects SET default_hours_per_day = COALESCE(default_hours_per_day, max_hours_per_day)',
    'SELECT 1'
);
PREPARE copy_legacy_max_hours_statement FROM @drop_legacy_max_hours_sql;
EXECUTE copy_legacy_max_hours_statement;
DEALLOCATE PREPARE copy_legacy_max_hours_statement;

SET @drop_legacy_max_hours_sql = IF(
    EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'timesheet_employee_projects'
          AND column_name = 'max_hours_per_day'
    ),
    'ALTER TABLE timesheet_employee_projects DROP COLUMN max_hours_per_day',
    'SELECT 1'
);
PREPARE drop_legacy_max_hours_statement FROM @drop_legacy_max_hours_sql;
EXECUTE drop_legacy_max_hours_statement;
DEALLOCATE PREPARE drop_legacy_max_hours_statement;