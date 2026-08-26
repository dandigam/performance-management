ALTER TABLE rate_cards
    CHANGE COLUMN effective_date effective_from DATE NOT NULL,
    ADD COLUMN effective_to DATE NULL AFTER effective_from;
