-- Cycle publication time in UTC, independent of final-rating publication.
-- Existing publication times are unknown and intentionally remain NULL.
-- MySQL does not support ADD COLUMN IF NOT EXISTS. Preserve manually applied changes.
SET @pms_migration_sql = IF(
    EXISTS (SELECT 1 FROM information_schema.columns
            WHERE table_schema = DATABASE() AND table_name = 'performance_cycles'
              AND column_name = 'published_date'),
    'SELECT 1',
    'ALTER TABLE performance_cycles ADD COLUMN published_date DATETIME(6) NULL');
PREPARE pms_migration_statement FROM @pms_migration_sql;
EXECUTE pms_migration_statement;
DEALLOCATE PREPARE pms_migration_statement;
