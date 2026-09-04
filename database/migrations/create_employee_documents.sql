CREATE TABLE IF NOT EXISTS employee_documents (
    employee_id BIGINT NOT NULL,
    document_id BIGINT NOT NULL,
    document_type_id BIGINT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_by BIGINT NULL,
    created_on DATETIME(6) NULL,
    updated_by BIGINT NULL,
    updated_on DATETIME(6) NULL,
    PRIMARY KEY (employee_id, document_id),
    CONSTRAINT uk_employee_documents UNIQUE (employee_id, document_id),
    CONSTRAINT fk_employee_documents_employee
        FOREIGN KEY (employee_id) REFERENCES employees (id),
    CONSTRAINT fk_employee_documents_document
        FOREIGN KEY (document_id) REFERENCES documents (id),
    CONSTRAINT fk_employee_documents_document_type
        FOREIGN KEY (document_type_id) REFERENCES lookup_values (id)
);

-- Upgrade an existing employee_documents join table in place.
ALTER TABLE employee_documents ADD COLUMN IF NOT EXISTS document_type_id BIGINT NULL AFTER document_id;
ALTER TABLE employee_documents ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE employee_documents ADD COLUMN IF NOT EXISTS created_by BIGINT NULL;
ALTER TABLE employee_documents ADD COLUMN IF NOT EXISTS created_on DATETIME(6) NULL;
ALTER TABLE employee_documents ADD COLUMN IF NOT EXISTS updated_by BIGINT NULL;
ALTER TABLE employee_documents ADD COLUMN IF NOT EXISTS updated_on DATETIME(6) NULL;
