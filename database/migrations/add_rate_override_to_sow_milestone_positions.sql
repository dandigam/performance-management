ALTER TABLE sow_milestone_positions
    ADD COLUMN hourly_rate DECIMAL(12,2) NULL AFTER rate_card_id,
    ADD COLUMN rate_override_reason VARCHAR(1000) NULL AFTER hourly_rate,
    ADD COLUMN rate_updated_by BIGINT NULL AFTER rate_override_reason,
    ADD COLUMN rate_updated_date DATETIME NULL AFTER rate_updated_by;

UPDATE sow_milestone_positions position
JOIN rate_cards rate_card ON rate_card.id = position.rate_card_id
SET position.hourly_rate = rate_card.hourly_rate
WHERE position.hourly_rate IS NULL;
