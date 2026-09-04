ALTER TABLE sow_invoices
    ADD COLUMN milestone_invoice_date DATE NULL AFTER milestone_id,
    ADD COLUMN milestone_invoice_amount DECIMAL(15, 2) NULL AFTER milestone_invoice_date,
    CHANGE COLUMN actual_invoice_date invoice_raised_date DATE NULL,
    CHANGE COLUMN invoice_amount invoice_raised_amount DECIMAL(15, 2) NULL,
    ADD COLUMN invoice_number VARCHAR(100) NULL AFTER submitted_date,
    ADD COLUMN notes VARCHAR(500) NULL AFTER invoice_number;

UPDATE sow_invoices invoice
JOIN sow_milestones milestone ON milestone.id = invoice.milestone_id
SET invoice.milestone_invoice_date = milestone.invoice_date,
    invoice.milestone_invoice_amount = milestone.amount
WHERE invoice.milestone_invoice_date IS NULL
   OR invoice.milestone_invoice_amount IS NULL;

UPDATE sow_invoices
SET invoice_number = CONCAT('INV-', LPAD(id, 6, '0'))
WHERE invoice_number IS NULL OR TRIM(invoice_number) = '';

ALTER TABLE sow_invoices
    ADD CONSTRAINT uk_sow_invoice_number UNIQUE (invoice_number);

CREATE TABLE sow_invoice_payments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    invoice_id BIGINT NOT NULL,
    payment_date DATE NOT NULL,
    received_amount DECIMAL(15, 2) NOT NULL,
    payment_reference VARCHAR(100) NULL,
    payment_method VARCHAR(50) NULL,
    notes VARCHAR(500) NULL,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    created_on DATETIME(6) NULL,
    updated_on DATETIME(6) NULL,
    PRIMARY KEY (id),
    INDEX idx_invoice_payments_invoice_id (invoice_id),
    CONSTRAINT fk_invoice_payment_invoice FOREIGN KEY (invoice_id)
        REFERENCES sow_invoices (id) ON DELETE CASCADE
);

INSERT INTO sow_invoice_payments (
    invoice_id, payment_date, received_amount, created_by, updated_by, created_on, updated_on)
SELECT id, payment_received_date, received_amount,
       created_by, updated_by, created_on, updated_on
FROM sow_invoices
WHERE payment_received_date IS NOT NULL
  AND received_amount IS NOT NULL
  AND received_amount > 0;

ALTER TABLE sow_invoices
    DROP INDEX idx_sow_invoices_payment_status,
    DROP COLUMN payment_received_date,
    DROP COLUMN received_amount,
    DROP COLUMN payment_status;

UPDATE sow_invoice_payments
SET payment_method = 'PAYMENT_RECEIVED';
