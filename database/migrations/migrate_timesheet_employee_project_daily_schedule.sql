-- Run this once before restarting the application after introducing milestone-based
-- timesheet project schedules. Hibernate cannot safely infer milestone values for
-- rows that existed before the milestone_id column was added.

-- Hibernate may have created milestone_id as NOT NULL with 0 for existing rows.
-- Make it nullable while legacy rows are matched and backfilled.
ALTER TABLE timesheet_employee_projects
    MODIFY COLUMN milestone_id BIGINT NULL;

-- Backfill only when exactly one milestone for the same SOW overlaps the configured
-- assignment range. Deliberately do not guess when multiple milestones match.
UPDATE timesheet_employee_projects tep
JOIN (
    SELECT legacy.id AS project_id, MIN(sm.id) AS milestone_id
    FROM timesheet_employee_projects legacy
    JOIN sow_milestones sm
      ON sm.sow_id = legacy.sow_id
     AND sm.start_date <= COALESCE(legacy.end_date, sm.end_date)
     AND sm.end_date >= legacy.start_date
    WHERE legacy.milestone_id IS NULL
       OR NOT EXISTS (
            SELECT 1
            FROM sow_milestones current_milestone
            WHERE current_milestone.id = legacy.milestone_id
              AND current_milestone.sow_id = legacy.sow_id
       )
    GROUP BY legacy.id
    HAVING COUNT(*) = 1
) matched ON matched.project_id = tep.id
SET tep.milestone_id = matched.milestone_id;

-- Review this result before continuing. Every row must have a valid milestone.
-- If rows are returned, map them explicitly, for example:
-- UPDATE timesheet_employee_projects SET milestone_id = 123 WHERE id = 456;
SELECT tep.id, tep.employee_id, tep.sow_id, tep.start_date, tep.end_date,
       tep.milestone_id
FROM timesheet_employee_projects tep
LEFT JOIN sow_milestones sm
  ON sm.id = tep.milestone_id AND sm.sow_id = tep.sow_id
WHERE sm.id IS NULL;

-- This intentionally fails if the preceding query still returns unresolved rows.
ALTER TABLE timesheet_employee_projects
    MODIFY COLUMN milestone_id BIGINT NOT NULL;

ALTER TABLE timesheet_employee_projects
    ADD CONSTRAINT fk_tep_milestone
        FOREIGN KEY (milestone_id) REFERENCES sow_milestones (id);

-- Backfill the renamed decimal setting for legacy configurations. Existing values
-- were constrained to 1..24, so zero identifies a newly-added unpopulated column.
UPDATE timesheet_employee_projects
SET default_hours_per_day = max_hours_per_day
WHERE default_hours_per_day = 0
  AND max_hours_per_day IS NOT NULL;
