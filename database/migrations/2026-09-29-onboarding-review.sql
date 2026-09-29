-- Apply once before starting the updated backend if Hibernate has not created these columns.
ALTER TABLE employee_onboardings
    ADD COLUMN review_comments TEXT NULL,
    ADD COLUMN reviewed_by VARCHAR(150) NULL,
    ADD COLUMN reviewed_at TIMESTAMP(6) NULL;

-- Includes the new event for installations using MySQL native enum columns.
ALTER TABLE email_notifications MODIFY COLUMN event_type ENUM(
    'ONBOARDING_INVITATION', 'ONBOARDING_CHANGES_REQUESTED', 'CYCLE_PUBLISHED',
    'ASSESSMENT_READY', 'ASSESSMENT_REOPENED', 'RESULT_PUBLISHED', 'REMINDER',
    'PASSWORD_CHANGED', 'MANUAL') NOT NULL;
