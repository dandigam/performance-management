CREATE TABLE positions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    position_title VARCHAR(150) NOT NULL,
    skills VARCHAR(500) NOT NULL,
    work_location VARCHAR(20) NOT NULL,
    seniority VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_date DATETIME NOT NULL,
    updated_date DATETIME NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE rate_cards (
    id BIGINT NOT NULL AUTO_INCREMENT,
    position_id BIGINT NOT NULL,
    client_id BIGINT NOT NULL,
    hourly_rate DECIMAL(12,2) NOT NULL,
    currency VARCHAR(20) NOT NULL,
    sow_type VARCHAR(20) NOT NULL,
    effective_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_date DATETIME NOT NULL,
    updated_date DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_rate_card_effective UNIQUE (client_id, position_id, sow_type, effective_date),
    CONSTRAINT fk_rate_card_position FOREIGN KEY (position_id) REFERENCES positions(id),
    CONSTRAINT fk_rate_card_client FOREIGN KEY (client_id) REFERENCES clients(id)
);
