CREATE TABLE IF NOT EXISTS clients (
    id BIGINT NOT NULL AUTO_INCREMENT,
    client_name VARCHAR(200) NOT NULL,
    client_address VARCHAR(1000) NOT NULL,
    procurement_person_name VARCHAR(150) NOT NULL,
    procurement_contact_number VARCHAR(30) NOT NULL,
    procurement_email VARCHAR(150) NOT NULL,
    invoice_submission_type VARCHAR(20) NOT NULL,
    invoice_submission_email VARCHAR(150) NULL,
    vmo_name VARCHAR(150) NOT NULL,
    vmo_contact_number VARCHAR(30) NOT NULL,
    vmo_email VARCHAR(150) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_date DATETIME(6) NOT NULL,
    updated_date DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_client_name UNIQUE (client_name)
);

CREATE TABLE IF NOT EXISTS client_documents (
    client_id BIGINT NOT NULL,
    document_id BIGINT NOT NULL,
    CONSTRAINT uk_client_documents UNIQUE (client_id, document_id),
    CONSTRAINT fk_client_documents_client
        FOREIGN KEY (client_id) REFERENCES clients (id),
    CONSTRAINT fk_client_documents_document
        FOREIGN KEY (document_id) REFERENCES documents (id)
);
