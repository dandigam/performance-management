ALTER TABLE documents
    ADD COLUMN document_type VARCHAR(255) NULL AFTER file_type;
