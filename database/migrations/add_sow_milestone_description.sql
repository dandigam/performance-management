ALTER TABLE sow_milestones
    ADD COLUMN description VARCHAR(2000) NULL AFTER milestone_name,
    MODIFY COLUMN start_date DATE NOT NULL,
    MODIFY COLUMN end_date DATE NOT NULL;
