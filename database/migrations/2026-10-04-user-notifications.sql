-- In-app inbox; separate from email delivery and email subscription configuration.
CREATE TABLE IF NOT EXISTS user_notifications (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    recipient_user_id BIGINT NOT NULL,
    category VARCHAR(40) NOT NULL,
    event_type VARCHAR(80) NOT NULL,
    title VARCHAR(200) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    related_record_type VARCHAR(40) NOT NULL,
    related_record_id BIGINT NOT NULL,
    created_on DATETIME(6) NOT NULL,
    read_at DATETIME(6) NULL,
    deduplication_key VARCHAR(255) NOT NULL,
    CONSTRAINT fk_user_notification_recipient FOREIGN KEY (recipient_user_id) REFERENCES users(id),
    CONSTRAINT uk_user_notification_event UNIQUE (recipient_user_id, deduplication_key),
    INDEX idx_user_notification_list (recipient_user_id, created_on, id),
    INDEX idx_user_notification_unread (recipient_user_id, read_at)
);
