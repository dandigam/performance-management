CREATE TABLE work_order_documents (
    work_order_id BIGINT NOT NULL,
    document_id BIGINT NOT NULL,
    CONSTRAINT uk_work_order_documents UNIQUE (work_order_id, document_id),
    CONSTRAINT fk_work_order_documents_work_order
        FOREIGN KEY (work_order_id) REFERENCES work_orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_work_order_documents_document
        FOREIGN KEY (document_id) REFERENCES documents (id)
);

CREATE INDEX idx_work_order_documents_document_id
    ON work_order_documents (document_id);
