ALTER TABLE vendors
    ADD COLUMN vendor_location VARCHAR(20) NULL AFTER company_name,
    ADD COLUMN vendor_type VARCHAR(50) NULL AFTER vendor_location,
    ADD COLUMN tax_identifier VARCHAR(50) NULL AFTER vendor_type,
    ADD CONSTRAINT uk_vendor_tax_identifier UNIQUE (tax_identifier);
