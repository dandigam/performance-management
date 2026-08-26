CREATE TABLE IF NOT EXISTS employee_addresses (
    id BIGINT NOT NULL AUTO_INCREMENT,
    employee_id BIGINT NOT NULL,
    address_line_1 VARCHAR(200) NULL,
    address_line_2 VARCHAR(200) NULL,
    city VARCHAR(100) NULL,
    state VARCHAR(100) NULL,
    postal_code VARCHAR(20) NULL,
    country VARCHAR(100) NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_employee_address_employee UNIQUE (employee_id),
    CONSTRAINT fk_employee_addresses_employee FOREIGN KEY (employee_id) REFERENCES employees (id)
);
