ALTER TABLE sow_milestone_positions
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'OPEN' AFTER position_type;

UPDATE sow_milestone_positions position_row
SET status = CASE
    WHEN EXISTS (
        SELECT 1
        FROM sow_milestone_position_assignments assignment_row
        WHERE assignment_row.milestone_position_id = position_row.id
          AND UPPER(assignment_row.status) = 'ACTIVE'
    ) THEN 'FILLED'
    WHEN (
        SELECT UPPER(assignment_row.status)
        FROM sow_milestone_position_assignments assignment_row
        WHERE assignment_row.milestone_position_id = position_row.id
        ORDER BY assignment_row.id DESC
        LIMIT 1
    ) = 'COMPLETED' THEN 'COMPLETED'
    ELSE 'OPEN'
END;
