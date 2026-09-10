CREATE TABLE timesheets (
    id BIGINT NOT NULL AUTO_INCREMENT,
    employee_id BIGINT NOT NULL,
    week_start_date DATE NOT NULL,
    week_end_date DATE NOT NULL,
    regular_hours DECIMAL(6, 2) NOT NULL DEFAULT 0,
    holiday_hours DECIMAL(6, 2) NOT NULL DEFAULT 0,
    leave_hours DECIMAL(6, 2) NOT NULL DEFAULT 0,
    total_hours DECIMAL(6, 2) NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    submitted_at DATETIME NULL,
    created_by BIGINT NULL,
    created_at DATETIME NULL,
    updated_by BIGINT NULL,
    updated_at DATETIME NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_timesheet_employee_week UNIQUE (employee_id, week_start_date),
    CONSTRAINT fk_timesheet_employee FOREIGN KEY (employee_id) REFERENCES employees (id),
    CONSTRAINT chk_timesheet_week CHECK (week_end_date = DATE_ADD(week_start_date, INTERVAL 6 DAY)),
    CONSTRAINT chk_timesheet_hours CHECK (
        regular_hours >= 0 AND holiday_hours >= 0 AND leave_hours >= 0 AND total_hours >= 0),
    CONSTRAINT chk_timesheet_status CHECK (
        status IN ('DRAFT', 'SUBMITTED', 'LEVEL1_APPROVED', 'REJECTED', 'APPROVED')),
    INDEX idx_timesheet_status (status)
);

CREATE TABLE timesheet_entries (
    id BIGINT NOT NULL AUTO_INCREMENT,
    timesheet_id BIGINT NOT NULL,
    work_date DATE NOT NULL,
    entry_type VARCHAR(20) NOT NULL,
    hours DECIMAL(5, 2) NOT NULL,
    job_id BIGINT NULL,
    sow_id BIGINT NULL,
    leave_id BIGINT NULL,
    holiday_id BIGINT NULL,
    created_by BIGINT NULL,
    created_at DATETIME NULL,
    updated_by BIGINT NULL,
    updated_at DATETIME NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_timesheet_entry_timesheet FOREIGN KEY (timesheet_id)
        REFERENCES timesheets (id) ON DELETE CASCADE,
    CONSTRAINT fk_timesheet_entry_holiday FOREIGN KEY (holiday_id) REFERENCES holidays (id),
    CONSTRAINT fk_timesheet_entry_sow FOREIGN KEY (sow_id) REFERENCES sows (id),
    CONSTRAINT chk_timesheet_entry_type CHECK (entry_type IN ('REGULAR', 'HOLIDAY', 'LEAVE')),
    CONSTRAINT chk_timesheet_entry_hours CHECK (hours >= 0 AND hours <= 24),
    INDEX idx_timesheet_entry_timesheet (timesheet_id),
    INDEX idx_timesheet_entry_work_date (work_date)
);

CREATE TABLE timesheet_approvals (
    id BIGINT NOT NULL AUTO_INCREMENT,
    timesheet_id BIGINT NOT NULL,
    approval_level INT NOT NULL,
    approver_employee_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    comments VARCHAR(2000) NULL,
    action_at DATETIME NULL,
    created_by BIGINT NULL,
    created_at DATETIME NULL,
    updated_by BIGINT NULL,
    updated_at DATETIME NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_timesheet_approval_level UNIQUE (timesheet_id, approval_level),
    CONSTRAINT fk_timesheet_approval_timesheet FOREIGN KEY (timesheet_id)
        REFERENCES timesheets (id) ON DELETE CASCADE,
    CONSTRAINT fk_timesheet_approval_approver FOREIGN KEY (approver_employee_id)
        REFERENCES employees (id),
    CONSTRAINT chk_timesheet_approval_level CHECK (approval_level IN (1, 2)),
    CONSTRAINT chk_timesheet_approval_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    INDEX idx_timesheet_approval_approver (approver_employee_id)
);

CREATE TABLE timesheet_employee_projects (
    id BIGINT NOT NULL AUTO_INCREMENT,
    employee_id BIGINT NOT NULL,
    sow_id BIGINT NOT NULL,
    milestone_id BIGINT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    default_hours_per_day DECIMAL(4, 2) NULL,
    level1_approver_id BIGINT NOT NULL,
    level2_approver_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_by BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uq_employee_project_milestone
        UNIQUE (employee_id, sow_id, milestone_id),
    CONSTRAINT fk_tep_employee FOREIGN KEY (employee_id) REFERENCES employees (id),
    CONSTRAINT fk_tep_sow FOREIGN KEY (sow_id) REFERENCES sows (id),
    CONSTRAINT fk_tep_milestone FOREIGN KEY (milestone_id) REFERENCES sow_milestones (id),
    CONSTRAINT fk_tep_level1_approver FOREIGN KEY (level1_approver_id) REFERENCES employees (id),
    CONSTRAINT fk_tep_level2_approver FOREIGN KEY (level2_approver_id) REFERENCES employees (id),
    CONSTRAINT chk_tep_dates CHECK (end_date IS NULL OR end_date >= start_date),
    CONSTRAINT chk_tep_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT chk_tep_approvers CHECK (
        employee_id <> level1_approver_id
        AND employee_id <> level2_approver_id
        AND level1_approver_id <> level2_approver_id),
    INDEX idx_tep_employee_status (employee_id, status),
    INDEX idx_tep_sow_status (sow_id, status),
    INDEX idx_tep_level1_approver (level1_approver_id),
    INDEX idx_tep_level2_approver (level2_approver_id)
);

CREATE TABLE timesheet_employee_project_day (
    id BIGINT NOT NULL AUTO_INCREMENT,
    timesheet_employee_project_id BIGINT NOT NULL,
    employee_id BIGINT NOT NULL,
    sow_id BIGINT NOT NULL,
    milestone_id BIGINT NOT NULL,
    work_date DATE NOT NULL,
    scheduled_hours DECIMAL(4, 2) NOT NULL DEFAULT 0,
    day_type VARCHAR(30) NOT NULL,
    holiday_id BIGINT NULL,
    work_schedule_id BIGINT NULL,
    locked BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_project_day_configuration FOREIGN KEY (timesheet_employee_project_id)
        REFERENCES timesheet_employee_projects (id),
    CONSTRAINT uq_project_work_date UNIQUE (timesheet_employee_project_id, work_date),
    INDEX idx_employee_work_date (employee_id, work_date),
    INDEX idx_sow_work_date (sow_id, work_date),
    INDEX idx_milestone_work_date (milestone_id, work_date)
);
