-- Apply once before deploying when Hibernate schema updates are disabled.
-- Existing SOWs retain NULL values until an owner/lead is selected.
ALTER TABLE sows
    ADD COLUMN delivery_owner_employee_id BIGINT NULL,
    ADD COLUMN technical_lead_employee_id BIGINT NULL,
    ADD CONSTRAINT fk_sow_delivery_owner FOREIGN KEY (delivery_owner_employee_id) REFERENCES employees(id),
    ADD CONSTRAINT fk_sow_technical_lead FOREIGN KEY (technical_lead_employee_id) REFERENCES employees(id);
