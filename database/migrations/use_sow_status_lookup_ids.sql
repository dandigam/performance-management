INSERT INTO lookup_types (code, name, description, is_active, created_on, updated_on)
SELECT 'SOW_STATUS', 'SOW Status', 'Lifecycle status for statements of work', 1, NOW(6), NOW(6)
WHERE NOT EXISTS (
    SELECT 1 FROM lookup_types WHERE UPPER(code) = 'SOW_STATUS'
);

INSERT INTO lookup_values
    (lookup_type_id, code, name, display_order, is_active, created_on, updated_on)
SELECT status_type.id, seed.code, seed.name, seed.display_order, 1, NOW(6), NOW(6)
FROM lookup_types status_type
JOIN (
    SELECT 'DRAFT' code, 'Draft' name, 1 display_order
    UNION ALL SELECT 'WAITING_FOR_APPROVAL', 'Waiting for Approval', 2
    UNION ALL SELECT 'ACTIVE', 'Active', 3
    UNION ALL SELECT 'ON_HOLD', 'On Hold', 4
    UNION ALL SELECT 'COMPLETED', 'Completed', 5
    UNION ALL SELECT 'CANCELLED', 'Cancelled', 6
) seed
WHERE UPPER(status_type.code) = 'SOW_STATUS'
  AND NOT EXISTS (
      SELECT 1 FROM lookup_values existing
      WHERE existing.lookup_type_id = status_type.id
        AND UPPER(existing.code) = seed.code
  );

ALTER TABLE sows ADD COLUMN status_id BIGINT NULL AFTER end_date;

UPDATE sows sow_row
JOIN lookup_types status_type ON UPPER(status_type.code) = 'SOW_STATUS'
JOIN lookup_values status_value
  ON status_value.lookup_type_id = status_type.id
 AND UPPER(status_value.code) = COALESCE(NULLIF(UPPER(TRIM(sow_row.status)), ''), 'DRAFT')
SET sow_row.status_id = status_value.id;

ALTER TABLE sows
    MODIFY status_id BIGINT NOT NULL,
    ADD INDEX idx_sow_status_id (status_id),
    ADD CONSTRAINT fk_sow_status FOREIGN KEY (status_id) REFERENCES lookup_values (id),
    DROP COLUMN status;
