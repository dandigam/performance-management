-- The entity now persists planned_start_date and planned_end_date.
-- Remove legacy non-null columns that are no longer mapped by JPA.
SET @drop_legacy_dates_sql = IF(
    EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'timesheet_employee_projects'
          AND column_name = 'planned_start_date'
    )
    AND EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'timesheet_employee_projects'
          AND column_name = 'start_date'
    ),
    'ALTER TABLE timesheet_employee_projects DROP COLUMN start_date, DROP COLUMN end_date',
    'SELECT 1'
);
PREPARE drop_legacy_dates_statement FROM @drop_legacy_dates_sql;
EXECUTE drop_legacy_dates_statement;
DEALLOCATE PREPARE drop_legacy_dates_statement;