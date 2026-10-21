-- Apply after 2026-09-27-user-portal-access.sql (MySQL 8).
ALTER TABLE users DROP CHECK chk_users_portal_access;
ALTER TABLE users ADD CONSTRAINT chk_users_portal_access
    CHECK (portal_access IN ('FULL', 'ONBOARDING', 'ONBOARDING_ONLY', 'BLOCKED'));
-- Hibernate maps @Enumerated strings to a native MySQL ENUM.
ALTER TABLE email_notifications MODIFY COLUMN event_type ENUM(
    'CYCLE_PUBLISHED', 'ASSESSMENT_READY', 'ASSESSMENT_REOPENED', 'RESULT_PUBLISHED',
    'REMINDER', 'PASSWORD_CHANGED', 'MANUAL', 'ONBOARDING_INVITATION') NOT NULL;

CREATE TABLE employee_onboardings (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NULL,
    request_actor VARCHAR(100) NOT NULL,
    idempotency_key VARCHAR(36) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    status VARCHAR(30) NOT NULL,
    required_sections TEXT NOT NULL,
    original_response TEXT NULL,
    invitation_token_id BIGINT NULL,
    invitation_notification_id BIGINT NULL,
    submitted_at TIMESTAMP(6) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_by BIGINT NULL,
    created_on DATETIME(6) NULL,
    updated_by BIGINT NULL,
    updated_on DATETIME(6) NULL,
    CONSTRAINT uk_onboarding_employee UNIQUE (employee_id),
    CONSTRAINT uk_onboarding_request UNIQUE (request_actor, idempotency_key),
    CONSTRAINT fk_onboarding_employee FOREIGN KEY (employee_id) REFERENCES employees(id),
    CONSTRAINT fk_onboarding_token FOREIGN KEY (invitation_token_id) REFERENCES password_reset_tokens(id),
    CONSTRAINT fk_onboarding_notification FOREIGN KEY (invitation_notification_id) REFERENCES email_notifications(id)
);
