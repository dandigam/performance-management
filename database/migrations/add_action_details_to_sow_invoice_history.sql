ALTER TABLE sow_invoice_history
    ADD COLUMN action_date DATE NULL,
    ADD COLUMN reason VARCHAR(1000) NULL,
    ADD COLUMN previous_status VARCHAR(30) NULL;
