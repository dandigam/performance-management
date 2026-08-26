CREATE TABLE sow_milestone_position_assignments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    employee_assignment_id BIGINT NOT NULL,
    milestone_position_id BIGINT NOT NULL,
    allocation_percentage INT NOT NULL,
    position_type VARCHAR(20) NOT NULL,
    assignment_start_date DATE NOT NULL,
    assignment_end_date DATE NULL,
    status VARCHAR(20) NOT NULL,
    created_by BIGINT NULL,
    created_date DATETIME(6) NOT NULL,
    updated_by BIGINT NULL,
    updated_date DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_smpa_employee_assignment_id (employee_assignment_id),
    INDEX idx_smpa_milestone_position_id (milestone_position_id),
    CONSTRAINT fk_smpa_employee_assignment
        FOREIGN KEY (employee_assignment_id) REFERENCES employee_assignments (id),
    CONSTRAINT fk_smpa_milestone_position
        FOREIGN KEY (milestone_position_id) REFERENCES sow_milestone_positions (id)
        ON DELETE CASCADE
);
