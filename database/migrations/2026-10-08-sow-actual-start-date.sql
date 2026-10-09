-- Apply before deployment when Hibernate schema updates are disabled.
ALTER TABLE sows ADD COLUMN actual_start_date DATE NULL;

-- Backfill from the first recorded non-baseline ACTIVE transition (chronological
-- event order, not the minimum effective date). Unknown historical starts stay null.
-- If Hibernate already created the column, run only this UPDATE.
UPDATE sows s
JOIN sow_status_history h ON h.sow_id = s.id
    AND UPPER(h.status) = 'ACTIVE' AND h.is_baseline = 0
LEFT JOIN sow_status_history earlier ON earlier.sow_id = h.sow_id
    AND UPPER(earlier.status) = 'ACTIVE' AND earlier.is_baseline = 0
    AND (earlier.changed_at < h.changed_at
         OR (earlier.changed_at = h.changed_at AND earlier.id < h.id))
SET s.actual_start_date = h.status_effective_date
WHERE s.actual_start_date IS NULL AND earlier.id IS NULL
    AND h.status_effective_date IS NOT NULL;
