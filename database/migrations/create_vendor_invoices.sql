CREATE TABLE vendor_invoices (
    id BIGINT NOT NULL AUTO_INCREMENT,
    invoice_number VARCHAR(100) NOT NULL,
    received_date DATE NOT NULL,
    invoice_type VARCHAR(50) NOT NULL,
    work_order_id BIGINT NOT NULL,
    vendor_id BIGINT NOT NULL,
    location VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_vendor_invoice_number UNIQUE (invoice_number),
    CONSTRAINT fk_vendor_invoice_work_order FOREIGN KEY (work_order_id) REFERENCES work_orders (id),
    CONSTRAINT fk_vendor_invoice_vendor FOREIGN KEY (vendor_id) REFERENCES vendors (id),
    INDEX idx_vendor_invoice_work_order_id (work_order_id),
    INDEX idx_vendor_invoice_vendor_id (vendor_id)
);
