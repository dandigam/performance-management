CREATE TABLE IF NOT EXISTS employee_professional_profiles (
    id BIGINT NOT NULL AUTO_INCREMENT,
    employee_id BIGINT NOT NULL,
    it_skills TEXT NULL,
    latest_experience TEXT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_employee_professional_profile UNIQUE (employee_id),
    CONSTRAINT fk_employee_professional_profiles_employee FOREIGN KEY (employee_id) REFERENCES employees (id)
);
