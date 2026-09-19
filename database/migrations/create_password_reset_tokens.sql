-- Apply once to an existing database if Hibernate schema updates are disabled.
ALTER TABLE users ADD COLUMN session_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE refresh_tokens ADD COLUMN session_version BIGINT NOT NULL DEFAULT 0;
CREATE TABLE password_reset_tokens (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    token_hash VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_password_reset_hash UNIQUE (token_hash),
    CONSTRAINT fk_password_reset_user FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_password_reset_user (user_id)
);
