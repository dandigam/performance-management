-- Run after milestone-position assignments have been populated and the updated
-- application is deployed. This deletes the redundant parent values permanently.
-- MySQL DDL commits implicitly. Foreign-key checks remain enabled.
DELIMITER $$
CREATE PROCEDURE drop_sow_assignment_duplicate_columns()
BEGIN
    DECLARE finished BOOLEAN DEFAULT FALSE;
    DECLARE constraint_name_to_drop VARCHAR(64);
    DECLARE column_name_to_drop VARCHAR(64);
    DECLARE foreign_keys CURSOR FOR
        SELECT DISTINCT CONSTRAINT_NAME
        FROM information_schema.KEY_COLUMN_USAGE
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sow_employee_assignments'
          AND COLUMN_NAME IN ('department_id', 'designation_id', 'position_type', 'milestone_id', 'allocation_percentage', 'is_primary_assignment')
          AND REFERENCED_TABLE_NAME IS NOT NULL;
    DECLARE obsolete_columns CURSOR FOR
        SELECT COLUMN_NAME FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sow_employee_assignments'
          AND COLUMN_NAME IN ('department_id', 'designation_id', 'position_type', 'milestone_id', 'allocation_percentage', 'is_primary_assignment');
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET finished = TRUE;

    OPEN foreign_keys;
    drop_keys: LOOP
        FETCH foreign_keys INTO constraint_name_to_drop;
        IF finished THEN LEAVE drop_keys; END IF;
        SET @drop_assignment_sql = CONCAT('ALTER TABLE sow_employee_assignments DROP FOREIGN KEY `',
                REPLACE(constraint_name_to_drop, '`', '``'), '`');
        PREPARE drop_assignment_statement FROM @drop_assignment_sql;
        EXECUTE drop_assignment_statement;
        DEALLOCATE PREPARE drop_assignment_statement;
    END LOOP;
    CLOSE foreign_keys;

    SET finished = FALSE;
    OPEN obsolete_columns;
    drop_columns: LOOP
        FETCH obsolete_columns INTO column_name_to_drop;
        IF finished THEN LEAVE drop_columns; END IF;
        SET @drop_assignment_sql = CONCAT('ALTER TABLE sow_employee_assignments DROP COLUMN `',
                REPLACE(column_name_to_drop, '`', '``'), '`');
        PREPARE drop_assignment_statement FROM @drop_assignment_sql;
        EXECUTE drop_assignment_statement;
        DEALLOCATE PREPARE drop_assignment_statement;
    END LOOP;
    CLOSE obsolete_columns;
END$$
DELIMITER ;
CALL drop_sow_assignment_duplicate_columns();
DROP PROCEDURE drop_sow_assignment_duplicate_columns;
