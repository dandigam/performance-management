-- Supports both script-created VARCHAR schemas and Hibernate-created ENUM schemas.
SET @tep_has_check = (SELECT COUNT(*) FROM information_schema.table_constraints
    WHERE constraint_schema = DATABASE() AND table_name = 'timesheet_employee_projects'
      AND constraint_name = 'chk_tep_status' AND constraint_type = 'CHECK');
SET @tep_sql = IF(@tep_has_check > 0,
    'ALTER TABLE timesheet_employee_projects DROP CHECK chk_tep_status', 'SELECT 1');
PREPARE tep_statement FROM @tep_sql;
EXECUTE tep_statement;
DEALLOCATE PREPARE tep_statement;

SET @tep_is_enum = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'timesheet_employee_projects'
      AND column_name = 'status' AND data_type = 'enum');
SET @tep_sql = IF(@tep_is_enum > 0,
    'ALTER TABLE timesheet_employee_projects MODIFY status ENUM(''ACTIVE'',''COMPLETED'',''INACTIVE'') NOT NULL DEFAULT ''ACTIVE''',
    'SELECT 1');
PREPARE tep_statement FROM @tep_sql;
EXECUTE tep_statement;
DEALLOCATE PREPARE tep_statement;

ALTER TABLE timesheet_employee_projects
    ADD CONSTRAINT chk_tep_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'COMPLETED'));
