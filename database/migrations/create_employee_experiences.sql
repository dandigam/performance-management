CREATE TABLE IF NOT EXISTS employee_experiences (
    id BIGINT NOT NULL AUTO_INCREMENT,
    employee_id BIGINT NOT NULL,
    company_name VARCHAR(200) NOT NULL,
    position VARCHAR(150) NOT NULL,
    location VARCHAR(150) NOT NULL,
    from_date DATE NOT NULL,
    end_date DATE NULL,
    created_by BIGINT NULL,
    created_on DATETIME(6) NULL,
    updated_by BIGINT NULL,
    updated_on DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_employee_experience_employee UNIQUE (employee_id),
    CONSTRAINT fk_employee_experiences_employee
        FOREIGN KEY (employee_id) REFERENCES employees (id),
    CONSTRAINT chk_employee_experience_dates
        CHECK (end_date IS NULL OR end_date >= from_date)
);
