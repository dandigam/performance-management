CREATE TABLE IF NOT EXISTS employee_leave_policies (
    id BIGINT NOT NULL AUTO_INCREMENT,
    employee_id BIGINT NOT NULL,
    leave_policy_id BIGINT NOT NULL,
    effective_from DATE NOT NULL,
    effective_to DATE NULL,
    status VARCHAR(10) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NULL,
    created_by BIGINT NULL,
    updated_at DATETIME NULL,
    updated_by BIGINT NULL,
    PRIMARY KEY (id),
    INDEX idx_employee_leave_policy_employee (employee_id),
    CONSTRAINT fk_employee_leave_policy_employee FOREIGN KEY (employee_id) REFERENCES employees(id),
    CONSTRAINT fk_employee_leave_policy_policy FOREIGN KEY (leave_policy_id) REFERENCES leave_policies(id)
) ENGINE=InnoDB;
