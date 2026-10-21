-- MySQL: apply before deploying the subscription configuration API.
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

-- The application initializer seeds NOTIFICATION_CATEGORY / ONBOARDING on startup.
-- Existing lookup values and their active status are preserved.
