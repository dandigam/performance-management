ALTER TABLE employee_assignments
    DROP COLUMN project_id,
    ADD COLUMN sow_id BIGINT NULL AFTER lead_id,
    ADD COLUMN milestone_id BIGINT NULL AFTER sow_id,
    ADD COLUMN position_type VARCHAR(20) NULL AFTER milestone_id,
    DROP COLUMN is_current,
    ADD COLUMN is_primary_assignment BOOLEAN NOT NULL DEFAULT FALSE AFTER status,
    ADD INDEX idx_employee_assignments_sow_id (sow_id),
    ADD INDEX idx_employee_assignments_milestone_id (milestone_id),
    ADD CONSTRAINT fk_employee_assignments_sow
        FOREIGN KEY (sow_id) REFERENCES sows (id),
    ADD CONSTRAINT fk_employee_assignments_milestone
        FOREIGN KEY (milestone_id) REFERENCES sow_milestones (id);

UPDATE employee_assignments assignment
JOIN (
    SELECT employee_id, MAX(id) AS primary_assignment_id
    FROM employee_assignments
    WHERE UPPER(status) = 'ACTIVE'
    GROUP BY employee_id
) current_assignment
    ON current_assignment.primary_assignment_id = assignment.id
SET assignment.is_primary_assignment = TRUE;
