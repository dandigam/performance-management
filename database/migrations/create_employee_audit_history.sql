CREATE TABLE IF NOT EXISTS employee_audit_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    employee_id BIGINT NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    action VARCHAR(30) NOT NULL,
    old_values LONGTEXT NULL,
    new_values LONGTEXT NULL,
    changed_by BIGINT NULL,
    changed_on DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_employee_audit_employee_changed (employee_id, changed_on),
    CONSTRAINT fk_employee_audit_employee FOREIGN KEY (employee_id) REFERENCES employees (id)
);
