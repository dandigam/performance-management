ALTER TABLE sow_employee_assignments
    DROP COLUMN project_id,
    ADD COLUMN sow_id BIGINT NULL AFTER lead_id,
    DROP COLUMN is_current,
    ADD INDEX idx_sow_employee_assignments_sow_id (sow_id),
    ADD CONSTRAINT fk_sow_employee_assignments_sow
        FOREIGN KEY (sow_id) REFERENCES sows (id);
