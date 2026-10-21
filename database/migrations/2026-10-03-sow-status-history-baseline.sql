-- Run after the status-history table migration. Preserve existing history.
SET @baseline_ddl = IF(
    EXISTS (SELECT 1 FROM information_schema.columns
            WHERE table_schema = DATABASE() AND table_name = 'sow_status_history'
              AND column_name = 'is_baseline'),
    'SELECT 1',
    'ALTER TABLE sow_status_history ADD COLUMN is_baseline BOOLEAN NOT NULL DEFAULT FALSE');
PREPARE baseline_stmt FROM @baseline_ddl;
EXECUTE baseline_stmt;
DEALLOCATE PREPARE baseline_stmt;
-- Old SOWs may have no effective date; retain that unknown value honestly.
ALTER TABLE sow_status_history MODIFY COLUMN status_effective_date DATE NULL;

START TRANSACTION;
-- Serialize with status PATCH requests, which also lock the SOW row.
SELECT id FROM sows ORDER BY id FOR UPDATE;
INSERT INTO sow_status_history
    (sow_id, previous_status, status, status_effective_date, changed_at,
     changed_by, approved_at, is_baseline)
SELECT s.id, NULL, v.code, s.status_effective_date,
       CURRENT_TIMESTAMP(6), NULL, NULL, TRUE
FROM sows s JOIN lookup_values v ON v.id = s.status_id
WHERE NOT EXISTS (SELECT 1 FROM sow_status_history h WHERE h.sow_id = s.id);
SELECT ROW_COUNT() AS baselines_inserted;
COMMIT;
