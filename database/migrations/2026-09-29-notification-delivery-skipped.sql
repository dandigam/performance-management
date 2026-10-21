-- Apply before deployment. Category-only messages with no configured recipients are skipped.
ALTER TABLE email_notifications MODIFY COLUMN status ENUM('PENDING','SENT','FAILED','SKIPPED') NOT NULL;
