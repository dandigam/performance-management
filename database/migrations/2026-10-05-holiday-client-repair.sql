-- MySQL repair for partially migrated / Hibernate-updated installations.
-- Select the application database before running. Safe to rerun.
-- No holiday rows or client assignments are changed.
SET @holiday_ddl = IF(
    EXISTS (SELECT 1 FROM information_schema.columns
            WHERE table_schema = DATABASE() AND table_name = 'holidays' AND column_name = 'client_id'),
    'SELECT 1', 'ALTER TABLE holidays ADD COLUMN client_id BIGINT NULL');
PREPARE holiday_stmt FROM @holiday_ddl;
EXECUTE holiday_stmt;
DEALLOCATE PREPARE holiday_stmt;

SET @holiday_ddl = IF(
    EXISTS (SELECT 1 FROM information_schema.statistics
            WHERE table_schema = DATABASE() AND table_name = 'holidays'
              AND index_name = 'uk_holiday_client_location_date'),
    'SELECT 1',
    'ALTER TABLE holidays ADD UNIQUE INDEX uk_holiday_client_location_date (client_id, location_type, holiday_date)');
PREPARE holiday_stmt FROM @holiday_ddl;
EXECUTE holiday_stmt;
DEALLOCATE PREPARE holiday_stmt;

SET @holiday_ddl = IF(
    EXISTS (SELECT 1 FROM information_schema.key_column_usage
            WHERE table_schema = DATABASE() AND table_name = 'holidays'
              AND column_name = 'client_id' AND referenced_table_name = 'clients'
              AND referenced_column_name = 'id'),
    'SELECT 1',
    'ALTER TABLE holidays ADD CONSTRAINT fk_holiday_client FOREIGN KEY (client_id) REFERENCES clients(id)');
PREPARE holiday_stmt FROM @holiday_ddl;
EXECUTE holiday_stmt;
DEALLOCATE PREPARE holiday_stmt;

-- Remove the obsolete restriction only after installing the client-specific one.
SET @holiday_ddl = IF(
    EXISTS (SELECT 1 FROM information_schema.statistics
            WHERE table_schema = DATABASE() AND table_name = 'holidays'
              AND index_name = 'uk_holiday_location_date'),
    'ALTER TABLE holidays DROP INDEX uk_holiday_location_date', 'SELECT 1');
PREPARE holiday_stmt FROM @holiday_ddl;
EXECUTE holiday_stmt;
DEALLOCATE PREPARE holiday_stmt;

SHOW INDEX FROM holidays;
