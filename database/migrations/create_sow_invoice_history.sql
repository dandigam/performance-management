CREATE TABLE sow_invoice_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    invoice_id BIGINT NOT NULL,
    milestone_invoice_date DATE NULL,
    milestone_invoice_amount DECIMAL(15,2) NULL,
    invoice_raised_date DATE NULL,
    invoice_raised_amount DECIMAL(15,2) NULL,
    invoice_number VARCHAR(100) NULL,
    invoice_status VARCHAR(30) NOT NULL,
    submitted_date DATE NULL,
    notes VARCHAR(500) NULL,
    action VARCHAR(30) NOT NULL,
    changed_by BIGINT NULL,
    changed_on DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_invoice_history_invoice_id (invoice_id),
    CONSTRAINT fk_invoice_history_invoice FOREIGN KEY (invoice_id)
        REFERENCES sow_invoices(id) ON DELETE CASCADE
);

CREATE TABLE sow_invoice_payment_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    invoice_id BIGINT NOT NULL,
    payment_id BIGINT NOT NULL,
    payment_date DATE NOT NULL,
    received_amount DECIMAL(15,2) NOT NULL,
    payment_reference VARCHAR(100) NULL,
    payment_method VARCHAR(50) NULL,
    notes VARCHAR(500) NULL,
    action VARCHAR(30) NOT NULL,
    changed_by BIGINT NULL,
    changed_on DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_payment_history_invoice_id (invoice_id),
    INDEX idx_payment_history_payment_id (payment_id),
    CONSTRAINT fk_payment_history_invoice FOREIGN KEY (invoice_id)
        REFERENCES sow_invoices(id) ON DELETE CASCADE
);
