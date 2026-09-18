-- Apply after add_timesheet_project_setup_reference.sql and use_setup_scoped_timesheet_weeks.sql.
ALTER TABLE timesheet_employee_project_day
    ADD COLUMN status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE';

-- Convert enum-backed installations as well as varchar-backed installations.
ALTER TABLE timesheets MODIFY COLUMN status VARCHAR(30) NOT NULL DEFAULT 'DRAFT';

-- Replace the status check when present (older installations may not have it).
SET @drop_timesheet_status_check = (
    SELECT IF(COUNT(*) > 0,
        'ALTER TABLE timesheets DROP CHECK chk_timesheet_status', 'SELECT 1')
    FROM information_schema.table_constraints
    WHERE constraint_schema = DATABASE() AND table_name = 'timesheets'
      AND constraint_name = 'chk_timesheet_status' AND constraint_type = 'CHECK'
);
PREPARE cancellation_stmt FROM @drop_timesheet_status_check;
EXECUTE cancellation_stmt;
DEALLOCATE PREPARE cancellation_stmt;
ALTER TABLE timesheets ADD CONSTRAINT chk_timesheet_status CHECK (
    status IN ('DRAFT', 'SUBMITTED', 'LEVEL1_APPROVED', 'REJECTED', 'APPROVED', 'CANCELLED'));
