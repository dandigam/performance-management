-- Apply once before deploying email OTP when Hibernate schema updates are disabled.
ALTER TABLE password_reset_tokens
    ADD COLUMN otp_hash VARCHAR(100) NULL,
    ADD COLUMN otp_expires_at DATETIME(6) NULL,
    ADD COLUMN otp_sent_at DATETIME(6) NULL,
    ADD COLUMN otp_attempts INT NOT NULL DEFAULT 0,
    ADD COLUMN otp_send_count INT NOT NULL DEFAULT 0;
