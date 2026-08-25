CREATE TABLE sow_invoices (
    id BIGINT NOT NULL AUTO_INCREMENT,
    sow_id BIGINT NOT NULL,
    milestone_id BIGINT NOT NULL,
    actual_invoice_date DATE NULL,
    invoice_amount DECIMAL(15, 2) NULL,
    invoice_status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    submitted_date DATE NULL,
    payment_received_date DATE NULL,
    received_amount DECIMAL(15, 2) NULL,
    payment_status VARCHAR(30) NOT NULL DEFAULT 'UNPAID',
    created_by BIGINT NULL,
    created_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT NULL,
    updated_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_sow_invoice_milestone UNIQUE (milestone_id),
    INDEX idx_sow_invoices_sow_id (sow_id),
    INDEX idx_sow_invoices_invoice_status (invoice_status),
    INDEX idx_sow_invoices_payment_status (payment_status),
    CONSTRAINT fk_sow_invoice_sow FOREIGN KEY (sow_id) REFERENCES sows (id) ON DELETE CASCADE,
    CONSTRAINT fk_sow_invoice_milestone FOREIGN KEY (milestone_id)
        REFERENCES sow_milestones (id) ON DELETE CASCADE
);

INSERT INTO sow_invoices (sow_id, milestone_id, invoice_status, payment_status)
SELECT milestone.sow_id, milestone.id, 'DRAFT', 'UNPAID'
FROM sow_milestones milestone
JOIN sows sow ON sow.id = milestone.sow_id
WHERE UPPER(sow.status) IN ('ACTIVE', 'START', 'STARTED');
