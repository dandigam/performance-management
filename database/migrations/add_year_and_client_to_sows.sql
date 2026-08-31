ALTER TABLE sows
    ADD COLUMN sow_year INT NULL AFTER sow_name,
    ADD COLUMN client_id BIGINT NULL AFTER sow_year,
    ADD INDEX idx_sow_client (client_id),
    ADD CONSTRAINT fk_sow_client FOREIGN KEY (client_id) REFERENCES clients(id);
