ALTER TABLE employees
    ADD COLUMN designation_id BIGINT NULL AFTER vendor_id,
    ADD INDEX idx_employees_designation_id (designation_id),
    ADD CONSTRAINT fk_employees_designation
        FOREIGN KEY (designation_id) REFERENCES lookup_values (id);

UPDATE employees employee
JOIN (
    SELECT assignment.employee_id, MIN(position.position_id) AS designation_id
    FROM sow_employee_assignments assignment
    JOIN sow_milestone_position_assignments detail ON detail.employee_assignment_id = assignment.id
    JOIN sow_milestone_positions position ON position.id = detail.milestone_position_id
    WHERE UPPER(assignment.status) = 'ACTIVE' AND UPPER(detail.status) = 'ACTIVE'
    GROUP BY assignment.employee_id
    HAVING COUNT(DISTINCT position.position_id) = 1
) current_designation
  ON current_designation.employee_id = employee.id
SET employee.designation_id = current_designation.designation_id
WHERE employee.designation_id IS NULL;
