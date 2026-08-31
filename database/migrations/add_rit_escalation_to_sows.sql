ALTER TABLE sows
    ADD COLUMN rit_escalation_employee_id BIGINT NULL,
    ADD CONSTRAINT fk_sow_rit_escalation
        FOREIGN KEY (rit_escalation_employee_id) REFERENCES employees (id);
