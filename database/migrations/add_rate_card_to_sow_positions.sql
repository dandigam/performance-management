ALTER TABLE sow_resource_allocations
    ADD COLUMN rate_card_id BIGINT NULL,
    ADD INDEX idx_sow_resource_allocation_rate_card_id (rate_card_id),
    ADD CONSTRAINT fk_sow_resource_allocation_rate_card
        FOREIGN KEY (rate_card_id) REFERENCES rate_cards (id);
