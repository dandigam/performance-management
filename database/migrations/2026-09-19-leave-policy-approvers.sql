-- Apply before deploying leave-policy approver validation.
-- Level 1 starts nullable so existing assignments can be populated safely.
ALTER TABLE employee_leave_policies
    ADD COLUMN level1_approver_id BIGINT NULL,
    ADD COLUMN level2_approver_id BIGINT NULL,
    ADD CONSTRAINT fk_employee_leave_policy_l1 FOREIGN KEY (level1_approver_id) REFERENCES employees(id),
    ADD CONSTRAINT fk_employee_leave_policy_l2 FOREIGN KEY (level2_approver_id) REFERENCES employees(id);

-- Populate level1_approver_id for existing assignments with the correct leave approver.
-- Do not copy timesheet approvers automatically.
-- After all existing rows are populated, enforce Level 1 at the database level:
-- ALTER TABLE employee_leave_policies MODIFY COLUMN level1_approver_id BIGINT NOT NULL;

-- For leave requests submitted before approver snapshots existed, review and populate
-- leave_requests.level1_approver_id / level2_approver_id from historical leave records.
-- A request with no Level 1 snapshot is not accessible to approvers until corrected.
