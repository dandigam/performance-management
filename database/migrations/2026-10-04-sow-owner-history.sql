-- Apply after the SOW owner columns migration. No cascading FKs: audit snapshots survive deletion.
CREATE TABLE IF NOT EXISTS sow_owner_history (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    sow_id BIGINT NOT NULL,
    role VARCHAR(30) NOT NULL,
    previous_employee_id BIGINT NULL,
    previous_employee_name VARCHAR(511) NULL,
    employee_id BIGINT NULL,
    employee_name VARCHAR(511) NULL,
    effective_date DATE NULL,
    reason VARCHAR(2000) NULL,
    changed_at DATETIME(6) NOT NULL,
    changed_by BIGINT NULL,
    is_baseline BOOLEAN NOT NULL DEFAULT FALSE,
    INDEX idx_sow_owner_history_sow (sow_id,id)
);

-- Serialize baseline capture with owner updates. Capture current owners, not invented past changes.
START TRANSACTION;
SELECT id FROM sows ORDER BY id FOR UPDATE;
INSERT INTO sow_owner_history
    (sow_id, role, employee_id, employee_name, changed_at, is_baseline, reason)
SELECT current_owner.sow_id, current_owner.role, current_owner.employee_id,
       TRIM(CONCAT(COALESCE(e.first_name, ''), ' ', COALESCE(e.last_name, ''))),
       CURRENT_TIMESTAMP(6), TRUE, 'Existing ownership snapshot; original effective date unknown'
FROM (
    SELECT id AS sow_id, 'DELIVERY_OWNER' AS role, delivery_owner_employee_id AS employee_id FROM sows
    UNION ALL
    SELECT id, 'TECHNICAL_LEAD', technical_lead_employee_id FROM sows
) current_owner
JOIN employees e ON e.id = current_owner.employee_id
WHERE NOT EXISTS (
    SELECT 1 FROM sow_owner_history h WHERE h.sow_id = current_owner.sow_id AND h.role = current_owner.role
);
COMMIT;
