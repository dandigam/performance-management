-- Additive migration. No historical transitions are inferred or backfilled.
-- No foreign key: retain audit history without changing SOW deletion behavior.
CREATE TABLE IF NOT EXISTS sow_status_history (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    sow_id BIGINT NOT NULL,
    previous_status VARCHAR(30) NULL,
    status VARCHAR(30) NOT NULL,
    status_effective_date DATE NOT NULL,
    changed_at DATETIME(6) NOT NULL,
    changed_by BIGINT NULL,
    approved_at DATETIME(6) NULL,
    INDEX idx_sow_status_history_sow (sow_id, id)
);
