ALTER TABLE employee_educations
    ADD INDEX idx_employee_education_employee (employee_id),
    DROP INDEX uk_employee_education_employee;
