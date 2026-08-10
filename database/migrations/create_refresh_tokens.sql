CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGINT NOT NULL AUTO_INCREMENT,
    token_hash VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    revoked_at DATETIME(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_refresh_token_hash (token_hash),
    INDEX idx_refresh_token_user (user_id),
    INDEX idx_refresh_token_expiry (expires_at),
    CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES users(id)
);
