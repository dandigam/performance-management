ALTER TABLE rate_cards
    ADD COLUMN skill VARCHAR(100) NULL AFTER position_title_id;

-- Populate existing records before changing the column to NOT NULL.
-- UPDATE rate_cards SET skill = 'General' WHERE skill IS NULL;
-- ALTER TABLE rate_cards MODIFY skill VARCHAR(100) NOT NULL;
