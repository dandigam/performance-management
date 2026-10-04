-- Run before deployment when automatic Hibernate schema updates are disabled.
CREATE TABLE IF NOT EXISTS office_locations (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 office_name VARCHAR(150) NOT NULL,
 address_line1 VARCHAR(255) NOT NULL,
 address_line2 VARCHAR(255) NULL,
 city VARCHAR(100) NOT NULL,
 state_region VARCHAR(100) NULL,
 postal_code VARCHAR(20) NULL,
 country_code VARCHAR(2) NOT NULL,
 time_zone VARCHAR(100) NOT NULL,
 phone VARCHAR(30) NULL,
 email VARCHAR(254) NULL,
 active BOOLEAN NOT NULL DEFAULT TRUE,
 created_by BIGINT NULL, created_on DATETIME(6) NULL,
 updated_by BIGINT NULL, updated_on DATETIME(6) NULL
);
CREATE TABLE IF NOT EXISTS company_settings (
 id BIGINT NOT NULL PRIMARY KEY,
 portal_name VARCHAR(150) NOT NULL,
 main_office_id BIGINT NULL,
 created_by BIGINT NULL, created_on DATETIME(6) NULL,
 updated_by BIGINT NULL, updated_on DATETIME(6) NULL,
 CONSTRAINT fk_company_main_office FOREIGN KEY (main_office_id) REFERENCES office_locations(id)
);
INSERT IGNORE INTO company_settings (id,portal_name,created_on,updated_on)
VALUES (1,'RailInfo Tech',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP);
