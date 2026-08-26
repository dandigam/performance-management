CREATE TABLE IF NOT EXISTS employee_documents (
    employee_id BIGINT NOT NULL,
    document_id BIGINT NOT NULL,
    CONSTRAINT uk_employee_documents UNIQUE (employee_id, document_id),
    CONSTRAINT fk_employee_documents_employee
        FOREIGN KEY (employee_id) REFERENCES employees (id),
    CONSTRAINT fk_employee_documents_document
        FOREIGN KEY (document_id) REFERENCES documents (id)
);
