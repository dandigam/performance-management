CREATE TABLE IF NOT EXISTS employee_educations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    employee_id BIGINT NOT NULL,
    education_type VARCHAR(100) NOT NULL,
    college_university VARCHAR(250) NOT NULL,
    passing_year INT NOT NULL,
    percentage DECIMAL(5,2) NOT NULL,
    created_by BIGINT NULL,
    created_on DATETIME(6) NULL,
    updated_by BIGINT NULL,
    updated_on DATETIME(6) NULL,
    PRIMARY KEY (id),
    INDEX idx_employee_education_employee (employee_id),
    CONSTRAINT fk_employee_educations_employee
        FOREIGN KEY (employee_id) REFERENCES employees (id),
    CONSTRAINT chk_employee_education_percentage
        CHECK (percentage >= 0 AND percentage <= 100),
    CONSTRAINT chk_employee_education_passing_year
        CHECK (passing_year >= 1900 AND passing_year <= 9999)
);
