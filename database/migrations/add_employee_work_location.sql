ALTER TABLE employees
    ADD COLUMN IF NOT EXISTS work_location VARCHAR(20) NULL AFTER work_mode;
