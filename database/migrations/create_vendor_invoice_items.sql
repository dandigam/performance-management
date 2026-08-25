CREATE TABLE vendor_invoices_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    vendor_invoice_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    description VARCHAR(1000) NOT NULL,
    unit_price DECIMAL(15,2) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_vendor_invoice_item_invoice_id (vendor_invoice_id),
    CONSTRAINT fk_vendor_invoice_item_invoice
        FOREIGN KEY (vendor_invoice_id) REFERENCES vendor_invoices (id)
        ON DELETE CASCADE
);
