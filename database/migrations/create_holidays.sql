CREATE TABLE IF NOT EXISTS holidays (
    id BIGINT NOT NULL AUTO_INCREMENT,
    holiday_name VARCHAR(150) NOT NULL,
    holiday_date DATE NOT NULL,
    location_type VARCHAR(20) NOT NULL,
    description VARCHAR(500) NULL,
    is_active BIT(1) NOT NULL DEFAULT 1,
    created_by BIGINT NULL,
    created_on DATETIME(6) NULL,
    updated_by BIGINT NULL,
    updated_on DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_holiday_location_date UNIQUE (location_type, holiday_date)
);
