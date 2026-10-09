-- Apply once before deployment when Hibernate automatic schema updates are disabled.
-- Existing history rows retain NULL reasons; do not fabricate historical explanations.
ALTER TABLE sow_status_history ADD COLUMN reason VARCHAR(2000) NULL;
