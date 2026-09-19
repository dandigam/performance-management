CREATE TABLE IF NOT EXISTS leave_requests (
    id BIGINT NOT NULL AUTO_INCREMENT,
    employee_id BIGINT NOT NULL,
    employee_leave_policy_id BIGINT NOT NULL,
    leave_type_id BIGINT NOT NULL,
    from_date DATE NOT NULL,
    to_date DATE NOT NULL,
    total_hours DECIMAL(10,2) NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    notes VARCHAR(2000) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    submitted_at DATETIME NULL,
    created_at DATETIME NULL,
    created_by BIGINT NULL,
    updated_at DATETIME NULL,
    updated_by BIGINT NULL,
    PRIMARY KEY (id),
    INDEX idx_leave_request_employee_status (employee_id, status),
    CONSTRAINT fk_leave_request_employee FOREIGN KEY (employee_id) REFERENCES employees(id),
    CONSTRAINT fk_leave_request_assignment FOREIGN KEY (employee_leave_policy_id) REFERENCES employee_leave_policies(id),
    CONSTRAINT fk_leave_request_type FOREIGN KEY (leave_type_id) REFERENCES leave_types(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS leave_request_days (
    id BIGINT NOT NULL AUTO_INCREMENT,
    leave_request_id BIGINT NOT NULL,
    leave_date DATE NOT NULL,
    scheduled_hours DECIMAL(5,2) NOT NULL,
    requested_hours DECIMAL(5,2) NOT NULL,
    created_at DATETIME NULL,
    created_by BIGINT NULL,
    updated_at DATETIME NULL,
    updated_by BIGINT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_leave_request_day_date UNIQUE (leave_request_id, leave_date),
    CONSTRAINT fk_leave_request_day_request FOREIGN KEY (leave_request_id) REFERENCES leave_requests(id)
) ENGINE=InnoDB;
