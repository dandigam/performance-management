CREATE TABLE IF NOT EXISTS leave_policies (
    id BIGINT NOT NULL AUTO_INCREMENT,
    policy_name VARCHAR(150) NOT NULL,
    country VARCHAR(10) NOT NULL,
    employment_type VARCHAR(20) NOT NULL,
    effective_from DATE NOT NULL,
    effective_to DATE NULL,
    status VARCHAR(10) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NULL,
    created_by BIGINT NULL,
    updated_at DATETIME NULL,
    updated_by BIGINT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS leave_policy_rules (
    id BIGINT NOT NULL AUTO_INCREMENT,
    leave_policy_id BIGINT NOT NULL,
    leave_type_id BIGINT NOT NULL,
    entitlement DECIMAL(10,2) NULL,
    carry_forward BOOLEAN NOT NULL DEFAULT FALSE,
    max_carry_forward DECIMAL(10,2) NULL,
    status VARCHAR(10) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NULL,
    created_by BIGINT NULL,
    updated_at DATETIME NULL,
    updated_by BIGINT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_leave_policy_rule_type UNIQUE (leave_policy_id, leave_type_id),
    CONSTRAINT fk_leave_policy_rule_policy FOREIGN KEY (leave_policy_id) REFERENCES leave_policies(id),
    CONSTRAINT fk_leave_policy_rule_type FOREIGN KEY (leave_type_id) REFERENCES leave_types(id)
) ENGINE=InnoDB;
