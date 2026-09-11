ALTER TABLE sow_invoices
    ADD COLUMN csx_invoice_number VARCHAR(100) NULL;

ALTER TABLE sow_invoice_history
    ADD COLUMN csx_invoice_number VARCHAR(100) NULL;

ALTER TABLE sow_invoice_payment_history
    ADD COLUMN csx_invoice_number VARCHAR(100) NULL;
