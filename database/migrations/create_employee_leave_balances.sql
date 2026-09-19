CREATE TABLE IF NOT EXISTS employee_leave_balances (
    id BIGINT NOT NULL AUTO_INCREMENT,
    employee_id BIGINT NOT NULL,
    employee_leave_policy_id BIGINT NOT NULL,
    leave_type_id BIGINT NOT NULL,
    balance_year INT NOT NULL,
    opening_balance DECIMAL(10,2) NOT NULL DEFAULT 0,
    entitled DECIMAL(10,2) NULL,
    used DECIMAL(10,2) NOT NULL DEFAULT 0,
    status VARCHAR(10) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NULL,
    created_by BIGINT NULL,
    updated_at DATETIME NULL,
    updated_by BIGINT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_employee_leave_balance_assignment_type_year UNIQUE (employee_leave_policy_id, leave_type_id, balance_year),
    INDEX idx_employee_leave_balance_employee_year (employee_id, balance_year),
    CONSTRAINT fk_employee_leave_balance_employee FOREIGN KEY (employee_id) REFERENCES employees(id),
    CONSTRAINT fk_employee_leave_balance_assignment FOREIGN KEY (employee_leave_policy_id) REFERENCES employee_leave_policies(id),
    CONSTRAINT fk_employee_leave_balance_type FOREIGN KEY (leave_type_id) REFERENCES leave_types(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS employee_leave_balance_adjustments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    employee_leave_balance_id BIGINT NOT NULL,
    adjustment_type VARCHAR(10) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    reason VARCHAR(255) NOT NULL,
    notes VARCHAR(1000) NULL,
    adjustment_date DATE NOT NULL,
    created_at DATETIME NULL,
    created_by BIGINT NULL,
    PRIMARY KEY (id),
    INDEX idx_employee_leave_balance_adjustment_balance (employee_leave_balance_id),
    CONSTRAINT fk_employee_leave_balance_adjustment_balance FOREIGN KEY (employee_leave_balance_id) REFERENCES employee_leave_balances(id)
) ENGINE=InnoDB;
