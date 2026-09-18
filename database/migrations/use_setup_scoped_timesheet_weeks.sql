-- Run after add_timesheet_project_setup_reference.sql.
-- Existing shared headers remain unchanged; generation preserves those weeks.
ALTER TABLE timesheets
    ADD INDEX idx_timesheet_employee (employee_id),
    DROP INDEX uk_timesheet_employee_week,
    ADD CONSTRAINT uk_timesheet_employee_setup_week
        UNIQUE (employee_id, timesheet_employee_project_id, week_start_date);
