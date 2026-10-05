-- Apply once before deployment when Hibernate schema updates are disabled.
-- Existing employees remain unassigned; select their client through the update API.
ALTER TABLE csx_employees
    ADD COLUMN client_id BIGINT NULL,
    ADD INDEX idx_csx_employee_client (client_id),
    ADD CONSTRAINT fk_csx_employee_client FOREIGN KEY (client_id) REFERENCES clients(id);
