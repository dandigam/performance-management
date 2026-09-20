-- Apply once to existing MySQL databases before deploying Step 6.
ALTER TABLE employee_leave_balances MODIFY COLUMN used DECIMAL(12,6) NOT NULL DEFAULT 0;

ALTER TABLE leave_requests
    ADD COLUMN level1_approver_id BIGINT NULL,
    ADD COLUMN level2_approver_id BIGINT NULL,
    ADD CONSTRAINT fk_leave_request_level1_approver FOREIGN KEY (level1_approver_id) REFERENCES employees(id),
    ADD CONSTRAINT fk_leave_request_level2_approver FOREIGN KEY (level2_approver_id) REFERENCES employees(id);

CREATE TABLE leave_request_approvals (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    leave_request_id BIGINT NOT NULL,
    approval_level VARCHAR(10) NOT NULL,
    approver_id BIGINT NOT NULL,
    action VARCHAR(10) NOT NULL,
    comments VARCHAR(2000) NULL,
    action_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NULL,
    created_by BIGINT NULL,
    CONSTRAINT fk_leave_approval_request FOREIGN KEY (leave_request_id) REFERENCES leave_requests(id),
    CONSTRAINT fk_leave_approval_employee FOREIGN KEY (approver_id) REFERENCES employees(id),
    INDEX idx_leave_approval_request (leave_request_id)
);
