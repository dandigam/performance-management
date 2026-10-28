-- MySQL 8+. Optional manual migration after resolving legacy stage mappings.
-- Keeps shared application roles and all assigned assessors/answers/results intact.
-- Back up first. Stop application writes while applying this transaction.
DROP PROCEDURE IF EXISTS migrate_performance_reporting_stages;
DELIMITER $$
CREATE PROCEDURE migrate_performance_reporting_stages()
BEGIN
    DECLARE stage_type BIGINT;
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        DROP TEMPORARY TABLE IF EXISTS performance_config_stage_map;
        DROP TEMPORARY TABLE IF EXISTS performance_assessment_stage_map;
        RESIGNAL;
    END;
    START TRANSACTION;
    INSERT INTO lookup_types (code, name, is_active)
    SELECT 'PERFORMANCE_STAGE', 'Performance Review Stage', 1
    WHERE NOT EXISTS (SELECT 1 FROM lookup_types WHERE code = 'PERFORMANCE_STAGE');
    SELECT id INTO stage_type FROM lookup_types WHERE code = 'PERFORMANCE_STAGE';
    INSERT INTO lookup_values (lookup_type_id, code, name, display_order, is_active)
    SELECT stage_type, stages.code, stages.name, stages.stage_order, 1
    FROM (
        SELECT 'SELF' AS code, 'Self Assessment' AS name, 1 AS stage_order
        UNION ALL SELECT 'REPORTING_MANAGER_1', 'Reporting Manager 1', 2
        UNION ALL SELECT 'REPORTING_MANAGER_2', 'Reporting Manager 2', 3
        UNION ALL SELECT 'HR', 'HR Publishing', 4
    ) stages
    WHERE NOT EXISTS (SELECT 1 FROM lookup_values existing
                      WHERE existing.lookup_type_id = stage_type AND existing.code = stages.code);

    -- Resolve old stage labels without renaming shared employee/application roles.
    -- Existing manager stages are mapped by their order within each cycle/review.
    CREATE TEMPORARY TABLE performance_config_stage_map AS
    SELECT source.*, CASE
        WHEN normalized IN ('EMPLOYEE','SELF','SELFASSESSOR') THEN 'SELF'
        WHEN normalized = 'HR' THEN 'HR'
        WHEN normalized = 'REPORTINGMANAGER1' THEN 'REPORTING_MANAGER_1'
        WHEN normalized = 'REPORTINGMANAGER2' THEN 'REPORTING_MANAGER_2'
        WHEN normalized IN ('TL','LEAD','TEAMLEAD','MANAGER','MANAGERSMANAGER')
            THEN CONCAT('REPORTING_MANAGER_', manager_number)
        ELSE NULL END AS stage_code
    FROM (
        SELECT config.id, config.performance_cycle_id AS cycle_id, config.role_id AS old_role_id,
               REGEXP_REPLACE(UPPER(role.code), '[^A-Z0-9]', '') AS normalized,
               SUM(CASE WHEN REGEXP_REPLACE(UPPER(role.code), '[^A-Z0-9]', '')
                   IN ('TL','LEAD','TEAMLEAD','MANAGER','MANAGERSMANAGER','REPORTINGMANAGER1','REPORTINGMANAGER2')
                   THEN 1 ELSE 0 END)
                   OVER (PARTITION BY config.performance_cycle_id ORDER BY config.display_order, config.id) AS manager_number
        FROM performance_cycle_assessors config JOIN lookup_values role ON role.id = config.role_id
    ) source;

    CREATE TEMPORARY TABLE performance_assessment_stage_map AS
    SELECT source.*, CASE
        WHEN normalized IN ('EMPLOYEE','SELF','SELFASSESSOR') THEN 'SELF'
        WHEN normalized = 'HR' THEN 'HR'
        WHEN normalized = 'REPORTINGMANAGER1' THEN 'REPORTING_MANAGER_1'
        WHEN normalized = 'REPORTINGMANAGER2' THEN 'REPORTING_MANAGER_2'
        WHEN normalized IN ('TL','LEAD','TEAMLEAD','MANAGER','MANAGERSMANAGER')
            THEN CONCAT('REPORTING_MANAGER_', manager_number)
        ELSE NULL END AS stage_code
    FROM (
        SELECT assessment.id, assessment.employee_review_id AS review_id,
               REGEXP_REPLACE(UPPER(role.code), '[^A-Z0-9]', '') AS normalized,
               SUM(CASE WHEN REGEXP_REPLACE(UPPER(role.code), '[^A-Z0-9]', '')
                   IN ('TL','LEAD','TEAMLEAD','MANAGER','MANAGERSMANAGER','REPORTINGMANAGER1','REPORTINGMANAGER2')
                   THEN 1 ELSE 0 END)
                   OVER (PARTITION BY assessment.employee_review_id ORDER BY assessment.assessment_level, assessment.id) AS manager_number
        FROM employee_review_assessments assessment JOIN lookup_values role ON role.id = assessment.assessor_role_id
    ) source;

    IF EXISTS (SELECT 1 FROM performance_config_stage_map WHERE stage_code IS NULL OR manager_number > 2)
       OR EXISTS (SELECT 1 FROM performance_assessment_stage_map WHERE stage_code IS NULL OR manager_number > 2) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Unsupported stage or more than two manager stages: review existing configuration before migration';
    END IF;
    IF EXISTS (SELECT 1 FROM performance_config_stage_map GROUP BY cycle_id, old_role_id HAVING COUNT(*) > 1) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Repeated stage role in a cycle: resolve ambiguous timeline mapping before migration';
    END IF;

    -- Preserve existing effective deadlines before changing role references.
    IF EXISTS (SELECT 1 FROM performance_config_stage_map GROUP BY cycle_id, stage_code HAVING COUNT(*) > 1)
       OR EXISTS (SELECT 1 FROM performance_assessment_stage_map GROUP BY review_id, stage_code HAVING COUNT(*) > 1) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Duplicate performance stages: resolve existing stage configuration before migration';
    END IF;

    UPDATE employee_review_assessments assessment
    JOIN employee_reviews review ON review.id = assessment.employee_review_id
    SET assessment.due_date = COALESCE(
        (SELECT timeline.end_date FROM performance_cycle_timelines timeline
         WHERE timeline.performance_cycle_id = review.cycle_id
           AND timeline.role_id = assessment.assessor_role_id
         ORDER BY timeline.display_order, timeline.id LIMIT 1),
        (SELECT timeline.end_date FROM performance_cycle_timelines timeline
         WHERE timeline.performance_cycle_id = review.cycle_id
           AND timeline.display_order = assessment.assessment_level
         ORDER BY timeline.display_order, timeline.id LIMIT 1))
    WHERE assessment.due_date IS NULL;

    UPDATE performance_cycle_timelines timeline
    JOIN performance_config_stage_map mapping
      ON mapping.cycle_id = timeline.performance_cycle_id AND mapping.old_role_id = timeline.role_id
    JOIN lookup_values stage ON stage.lookup_type_id = stage_type AND stage.code = mapping.stage_code
    SET timeline.role_id = stage.id;
    UPDATE performance_cycle_assessors config
    JOIN performance_config_stage_map mapping ON mapping.id = config.id
    JOIN lookup_values stage ON stage.lookup_type_id = stage_type AND stage.code = mapping.stage_code
    SET config.role_id = stage.id, config.assessor_name = stage.name;
    UPDATE employee_review_assessments assessment
    JOIN performance_assessment_stage_map mapping ON mapping.id = assessment.id
    JOIN lookup_values stage ON stage.lookup_type_id = stage_type AND stage.code = mapping.stage_code
    SET assessment.assessor_role_id = stage.id;
    DROP TEMPORARY TABLE performance_config_stage_map;
    DROP TEMPORARY TABLE performance_assessment_stage_map;
    COMMIT;
END$$
DELIMITER ;
CALL migrate_performance_reporting_stages();
DROP PROCEDURE migrate_performance_reporting_stages;
