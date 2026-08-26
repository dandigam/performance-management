CREATE TABLE IF NOT EXISTS employee_compensations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    employee_id BIGINT NOT NULL,
    pay_type VARCHAR(30) NOT NULL,
    hourly_rate DECIMAL(12,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    effective_date DATE NOT NULL,
    is_current BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_employee_compensation_employee (employee_id),
    INDEX idx_employee_compensation_current (employee_id, is_current),
    CONSTRAINT fk_employee_compensations_employee FOREIGN KEY (employee_id) REFERENCES employees (id)
);
