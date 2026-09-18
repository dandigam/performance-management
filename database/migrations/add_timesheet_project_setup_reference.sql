-- Optional reference: existing employee/week rows may cover multiple project setups.
-- Leave existing rows unlinked rather than choosing an ambiguous setup.
ALTER TABLE timesheets
    ADD COLUMN timesheet_employee_project_id BIGINT NULL AFTER employee_id,
    ADD CONSTRAINT fk_timesheet_project_setup
        FOREIGN KEY (timesheet_employee_project_id) REFERENCES timesheet_employee_projects (id);
