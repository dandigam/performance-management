-- MySQL 8. Run with the backend stopped, before deploying the new entity mappings.
-- No setup or schedule records are deleted. Existing planned dates are preserved.
DELIMITER $$
CREATE PROCEDURE migrate_timesheet_assignment_dates()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE()
        AND table_name='timesheet_employee_projects' AND column_name='planned_start_date') THEN
        ALTER TABLE timesheet_employee_projects RENAME COLUMN start_date TO planned_start_date;
        ALTER TABLE timesheet_employee_projects RENAME COLUMN end_date TO planned_end_date;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE()
        AND table_name='timesheet_employee_projects' AND column_name='work_type') THEN
        ALTER TABLE timesheet_employee_projects ADD work_type VARCHAR(20) NOT NULL DEFAULT 'PROJECT';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE()
        AND table_name='timesheet_employee_projects' AND column_name='internal_work_type') THEN
        ALTER TABLE timesheet_employee_projects ADD internal_work_type VARCHAR(50) NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE()
        AND table_name='timesheet_employee_projects' AND column_name='milestone_position_assignment_id') THEN
        ALTER TABLE timesheet_employee_projects ADD milestone_position_assignment_id BIGINT NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE()
        AND table_name='timesheet_employee_projects' AND column_name='assignment_start_date') THEN
        ALTER TABLE timesheet_employee_projects ADD assignment_start_date DATE NULL;
        ALTER TABLE timesheet_employee_projects ADD assignment_end_date DATE NULL;
    END IF;
    ALTER TABLE timesheet_employee_projects MODIFY sow_id BIGINT NULL, MODIFY milestone_id BIGINT NULL;
    ALTER TABLE timesheet_employee_project_day MODIFY sow_id BIGINT NULL, MODIFY milestone_id BIGINT NULL;
    UPDATE timesheet_employee_projects SET work_type='PROJECT' WHERE work_type IS NULL OR TRIM(work_type)='';

    -- Only link an existing setup when exactly one assignment period overlaps its planned range.
    UPDATE timesheet_employee_projects setup
    JOIN (
        SELECT t.id, MIN(a.id) AS assignment_id
        FROM (SELECT * FROM timesheet_employee_projects) t
        JOIN sow_employee_assignments parent ON parent.employee_id=t.employee_id AND parent.sow_id=t.sow_id
        JOIN sow_milestone_position_assignments a ON a.employee_assignment_id=parent.id
        JOIN sow_milestone_positions p ON p.id=a.milestone_position_id
            AND p.sow_id=t.sow_id AND p.milestone_id=t.milestone_id
        WHERE t.work_type='PROJECT' AND t.milestone_position_assignment_id IS NULL
          AND a.assignment_start_date <= t.planned_end_date
          AND (a.assignment_end_date IS NULL OR a.assignment_end_date >= t.planned_start_date)
        GROUP BY t.id HAVING COUNT(*)=1
    ) match_row ON match_row.id=setup.id
    SET setup.milestone_position_assignment_id=match_row.assignment_id;

    UPDATE timesheet_employee_projects t
    JOIN sow_milestone_position_assignments a ON a.id=t.milestone_position_assignment_id
    SET t.assignment_start_date=a.assignment_start_date,
        t.assignment_end_date=CASE WHEN UPPER(a.status)='COMPLETED' THEN a.assignment_end_date ELSE NULL END,
        t.status=CASE WHEN UPPER(a.status)='COMPLETED' THEN 'COMPLETED' ELSE t.status END;
    -- For unresolved legacy rows, do not invent an actual completion date.
    UPDATE timesheet_employee_projects SET assignment_start_date=planned_start_date
        WHERE assignment_start_date IS NULL;

    IF EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE()
        AND table_name='timesheet_employee_projects' AND index_name='uq_employee_project_milestone') THEN
        ALTER TABLE timesheet_employee_projects DROP INDEX uq_employee_project_milestone;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE()
        AND table_name='timesheet_employee_projects' AND index_name='uq_timesheet_position_assignment') THEN
        ALTER TABLE timesheet_employee_projects ADD CONSTRAINT uq_timesheet_position_assignment UNIQUE (milestone_position_assignment_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.key_column_usage WHERE constraint_schema=DATABASE()
        AND table_name='timesheet_employee_projects' AND column_name='milestone_position_assignment_id'
        AND referenced_table_name='sow_milestone_position_assignments') THEN
        ALTER TABLE timesheet_employee_projects ADD CONSTRAINT fk_tep_resource_assignment
            FOREIGN KEY (milestone_position_assignment_id) REFERENCES sow_milestone_position_assignments(id);
    END IF;
END$$
DELIMITER ;
CALL migrate_timesheet_assignment_dates();
DROP PROCEDURE migrate_timesheet_assignment_dates;

-- Any returned rows need assignment matching reviewed before editing/completing their setup.
SELECT id, employee_id, sow_id, milestone_id, planned_start_date, planned_end_date
FROM timesheet_employee_projects
WHERE work_type='PROJECT' AND milestone_position_assignment_id IS NULL;
