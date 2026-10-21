-- MySQL 8.0.16+. Select the application database; stop on the first error.
-- Prerequisites and rollout: docs/production-release-2026-09-29.md

DELIMITER $$
DROP PROCEDURE IF EXISTS pms_release_20260929$$
CREATE PROCEDURE pms_release_20260929()
BEGIN
    IF DATABASE() IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Select the application database first';
    END IF;
    IF (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE()
        AND table_name IN ('employees','users','lookup_types','lookup_values','password_reset_tokens','email_notifications')) <> 6 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing baseline tables: apply the existing application schema first';
    END IF;
    IF EXISTS (SELECT 1 FROM employees WHERE email IS NOT NULL
               GROUP BY LOWER(TRIM(email)) HAVING COUNT(*) > 1) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Duplicate normalized employee emails: resolve before migration';
    END IF;
    IF EXISTS (SELECT 1 FROM employees WHERE NULLIF(TRIM(phone_number),'') IS NOT NULL
               GROUP BY TRIM(phone_number) HAVING COUNT(*) > 1) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Duplicate normalized employee phones: resolve before migration';
    END IF;
    IF EXISTS (SELECT 1 FROM email_notifications WHERE event_type IS NULL OR event_type NOT IN
        ('ONBOARDING_INVITATION','ONBOARDING_CHANGES_REQUESTED','ONBOARDING_SUBMITTED',
         'CYCLE_PUBLISHED','ASSESSMENT_READY','ASSESSMENT_REOPENED','RESULT_PUBLISHED','REMINDER','PASSWORD_CHANGED','MANUAL'))
       OR EXISTS (SELECT 1 FROM email_notifications WHERE status IS NULL OR status NOT IN ('PENDING','SENT','FAILED','SKIPPED')) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Unexpected email enum data: review before changing enum definitions';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
               AND table_name = 'users' AND column_name = 'portal_access') THEN
        IF EXISTS (SELECT 1 FROM users WHERE portal_access IS NULL OR portal_access NOT IN ('FULL','ONBOARDING','ONBOARDING_ONLY','BLOCKED')) THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Unexpected portal_access data: resolve before migration';
        END IF;
    END IF;

    UPDATE employees SET email = LOWER(TRIM(email)) WHERE email IS NOT NULL AND BINARY email <> BINARY LOWER(TRIM(email));
    UPDATE employees SET phone_number = NULLIF(TRIM(phone_number),'')
        WHERE phone_number IS NOT NULL AND (TRIM(phone_number) = '' OR BINARY phone_number <> BINARY TRIM(phone_number));
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'employees' AND non_unique = 0
        GROUP BY index_name HAVING COUNT(*) = 1 AND MAX(column_name) = 'email' AND MAX(sub_part) IS NULL) THEN
        ALTER TABLE employees ADD CONSTRAINT uk_employee_email UNIQUE (email);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'employees' AND non_unique = 0
        GROUP BY index_name HAVING COUNT(*) = 1 AND MAX(column_name) = 'phone_number' AND MAX(sub_part) IS NULL) THEN
        ALTER TABLE employees ADD CONSTRAINT uk_employee_phone_number UNIQUE (phone_number);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
                   AND table_name = 'users' AND column_name = 'portal_access') THEN
        ALTER TABLE users ADD COLUMN portal_access VARCHAR(20) NOT NULL DEFAULT 'FULL';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_schema = DATABASE()
               AND table_name = 'users' AND constraint_name = 'chk_users_portal_access' AND constraint_type = 'CHECK') THEN
        ALTER TABLE users DROP CHECK chk_users_portal_access;
    END IF;
    ALTER TABLE users ADD CONSTRAINT chk_users_portal_access
        CHECK (portal_access IN ('FULL','ONBOARDING','ONBOARDING_ONLY','BLOCKED'));
CREATE TABLE IF NOT EXISTS employee_onboardings (
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

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
                   AND table_name = 'employee_onboardings' AND column_name = 'review_comments') THEN
        ALTER TABLE employee_onboardings ADD COLUMN review_comments TEXT NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
                   AND table_name = 'employee_onboardings' AND column_name = 'reviewed_by') THEN
        ALTER TABLE employee_onboardings ADD COLUMN reviewed_by VARCHAR(150) NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
                   AND table_name = 'employee_onboardings' AND column_name = 'reviewed_at') THEN
        ALTER TABLE employee_onboardings ADD COLUMN reviewed_at TIMESTAMP(6) NULL;
    END IF;
CREATE TABLE IF NOT EXISTS notification_subscriptions (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    category_id BIGINT NOT NULL,
    email_addresses VARCHAR(10000) NOT NULL,
    recipient_type VARCHAR(3) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by BIGINT NULL,
    created_on DATETIME(6) NULL,
    updated_by BIGINT NULL,
    updated_on DATETIME(6) NULL,
    CONSTRAINT uk_notification_subscription_category UNIQUE (category_id),
    CONSTRAINT fk_notification_subscription_category FOREIGN KEY (category_id) REFERENCES lookup_values(id),
    CONSTRAINT ck_notification_subscription_recipient_type CHECK (recipient_type IN ('CC', 'BCC'))
);
IF NOT EXISTS (SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'email_notifications'
      AND column_name = 'event_type' AND is_nullable = 'NO'
      AND column_type = 'enum(''ONBOARDING_INVITATION'',''ONBOARDING_CHANGES_REQUESTED'',''ONBOARDING_SUBMITTED'',''CYCLE_PUBLISHED'',''ASSESSMENT_READY'',''ASSESSMENT_REOPENED'',''RESULT_PUBLISHED'',''REMINDER'',''PASSWORD_CHANGED'',''MANUAL'')') THEN
ALTER TABLE email_notifications MODIFY COLUMN event_type ENUM(
    'ONBOARDING_INVITATION', 'ONBOARDING_CHANGES_REQUESTED', 'ONBOARDING_SUBMITTED',
    'CYCLE_PUBLISHED', 'ASSESSMENT_READY', 'ASSESSMENT_REOPENED', 'RESULT_PUBLISHED',
    'REMINDER', 'PASSWORD_CHANGED', 'MANUAL') NOT NULL;
END IF;
IF NOT EXISTS (SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'email_notifications'
      AND column_name = 'status' AND is_nullable = 'NO'
      AND column_type = 'enum(''PENDING'',''SENT'',''FAILED'',''SKIPPED'')') THEN
ALTER TABLE email_notifications MODIFY COLUMN status ENUM('PENDING','SENT','FAILED','SKIPPED') NOT NULL;
END IF;

END$$
CALL pms_release_20260929()$$
DROP PROCEDURE pms_release_20260929$$
DELIMITER ;
START TRANSACTION;

INSERT INTO lookup_types (code, name, description, is_active)
SELECT 'NOTIFICATION_CATEGORY', 'Notification Category', 'Business notification subscription categories', TRUE
WHERE NOT EXISTS (SELECT 1 FROM lookup_types WHERE UPPER(code) = 'NOTIFICATION_CATEGORY');

INSERT INTO lookup_values (lookup_type_id, code, name, description, display_order, is_active)
SELECT t.id, 'ALL_NOTIFICATIONS', 'All Notifications', 'All Notifications notification subscriptions', 1, TRUE
FROM lookup_types t
WHERE UPPER(t.code) = 'NOTIFICATION_CATEGORY'
  AND NOT EXISTS (SELECT 1 FROM lookup_values v WHERE v.lookup_type_id = t.id AND UPPER(v.code) = 'ALL_NOTIFICATIONS');

INSERT INTO lookup_values (lookup_type_id, code, name, description, display_order, is_active)
SELECT t.id, 'ONBOARDING', 'Onboarding', 'Onboarding notification subscriptions', 2, TRUE
FROM lookup_types t
WHERE UPPER(t.code) = 'NOTIFICATION_CATEGORY'
  AND NOT EXISTS (SELECT 1 FROM lookup_values v WHERE v.lookup_type_id = t.id AND UPPER(v.code) = 'ONBOARDING');

INSERT INTO lookup_values (lookup_type_id, code, name, description, display_order, is_active)
SELECT t.id, 'LEAVE', 'Leave', 'Leave notification subscriptions', 3, TRUE
FROM lookup_types t
WHERE UPPER(t.code) = 'NOTIFICATION_CATEGORY'
  AND NOT EXISTS (SELECT 1 FROM lookup_values v WHERE v.lookup_type_id = t.id AND UPPER(v.code) = 'LEAVE');

INSERT INTO lookup_values (lookup_type_id, code, name, description, display_order, is_active)
SELECT t.id, 'TIMESHEET', 'Timesheet', 'Timesheet notification subscriptions', 4, TRUE
FROM lookup_types t
WHERE UPPER(t.code) = 'NOTIFICATION_CATEGORY'
  AND NOT EXISTS (SELECT 1 FROM lookup_values v WHERE v.lookup_type_id = t.id AND UPPER(v.code) = 'TIMESHEET');

INSERT INTO lookup_values (lookup_type_id, code, name, description, display_order, is_active)
SELECT t.id, 'PERFORMANCE_REVIEW', 'Performance Review', 'Performance Review notification subscriptions', 5, TRUE
FROM lookup_types t
WHERE UPPER(t.code) = 'NOTIFICATION_CATEGORY'
  AND NOT EXISTS (SELECT 1 FROM lookup_values v WHERE v.lookup_type_id = t.id AND UPPER(v.code) = 'PERFORMANCE_REVIEW');

INSERT INTO lookup_values (lookup_type_id, code, name, description, display_order, is_active)
SELECT t.id, 'SOW', 'Statement of Work', 'Statement of Work notification subscriptions', 6, TRUE
FROM lookup_types t
WHERE UPPER(t.code) = 'NOTIFICATION_CATEGORY'
  AND NOT EXISTS (SELECT 1 FROM lookup_values v WHERE v.lookup_type_id = t.id AND UPPER(v.code) = 'SOW');

INSERT INTO lookup_values (lookup_type_id, code, name, description, display_order, is_active)
SELECT t.id, 'INVOICE', 'Invoice and Payments', 'Invoice and Payments notification subscriptions', 7, TRUE
FROM lookup_types t
WHERE UPPER(t.code) = 'NOTIFICATION_CATEGORY'
  AND NOT EXISTS (SELECT 1 FROM lookup_values v WHERE v.lookup_type_id = t.id AND UPPER(v.code) = 'INVOICE');

INSERT INTO lookup_values (lookup_type_id, code, name, description, display_order, is_active)
SELECT t.id, 'RESOURCE_ALLOCATION', 'Resource Allocation', 'Resource Allocation notification subscriptions', 8, TRUE
FROM lookup_types t
WHERE UPPER(t.code) = 'NOTIFICATION_CATEGORY'
  AND NOT EXISTS (SELECT 1 FROM lookup_values v WHERE v.lookup_type_id = t.id AND UPPER(v.code) = 'RESOURCE_ALLOCATION');

COMMIT;
