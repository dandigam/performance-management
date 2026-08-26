ALTER TABLE employees
    ADD COLUMN designation_id BIGINT NULL AFTER vendor_id,
    ADD INDEX idx_employees_designation_id (designation_id),
    ADD CONSTRAINT fk_employees_designation
        FOREIGN KEY (designation_id) REFERENCES lookup_values (id);

UPDATE employees employee
JOIN (
    SELECT assignment.employee_id, assignment.designation_id
    FROM employee_assignments assignment
    JOIN (
        SELECT employee_id, MAX(id) AS assignment_id
        FROM employee_assignments
        WHERE UPPER(status) = 'ACTIVE'
          AND designation_id IS NOT NULL
        GROUP BY employee_id
    ) latest
      ON latest.assignment_id = assignment.id
) current_designation
  ON current_designation.employee_id = employee.id
SET employee.designation_id = current_designation.designation_id
WHERE employee.designation_id IS NULL;
