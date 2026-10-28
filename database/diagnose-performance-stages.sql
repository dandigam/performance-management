-- Read-only MySQL 8+ diagnostics for the production 20261027.2 migration failure.
-- Run against the affected application database. No data or history is modified.
WITH stage_rows AS (
    SELECT 'cycle_config' AS source_table, config.id AS record_id,
           config.performance_cycle_id AS context_id, config.role_id AS role_id,
           role.code AS role_code, role.name AS role_name,
           config.display_order AS stage_order
    FROM performance_cycle_assessors config
    JOIN lookup_values role ON role.id = config.role_id
    UNION ALL
    SELECT 'review_assessment', assessment.id, assessment.employee_review_id,
           assessment.assessor_role_id, role.code, role.name,
           assessment.assessment_level
    FROM employee_review_assessments assessment
    JOIN lookup_values role ON role.id = assessment.assessor_role_id
), normalized_rows AS (
    SELECT stage_rows.*,
           REGEXP_REPLACE(UPPER(role_code), '[^A-Z0-9]', '') AS normalized
    FROM stage_rows
), numbered_rows AS (
    SELECT normalized_rows.*,
           SUM(CASE WHEN normalized IN (
               'TL','LEAD','TEAMLEAD','MANAGER','MANAGERSMANAGER',
               'REPORTINGMANAGER1','REPORTINGMANAGER2') THEN 1 ELSE 0 END)
           OVER (PARTITION BY source_table, context_id ORDER BY stage_order, record_id)
           AS manager_number
    FROM normalized_rows
), mapped_rows AS (
    SELECT numbered_rows.*,
           CASE
               WHEN normalized IN ('EMPLOYEE','SELF','SELFASSESSOR') THEN 'SELF'
               WHEN normalized = 'HR' THEN 'HR'
               WHEN normalized = 'REPORTINGMANAGER1' THEN 'REPORTING_MANAGER_1'
               WHEN normalized = 'REPORTINGMANAGER2' THEN 'REPORTING_MANAGER_2'
               WHEN normalized IN ('TL','LEAD','TEAMLEAD','MANAGER','MANAGERSMANAGER')
                   THEN CONCAT('REPORTING_MANAGER_', manager_number)
               ELSE NULL
           END AS proposed_stage
    FROM numbered_rows
)
SELECT source_table, context_id, record_id, role_id, role_code, role_name,
       normalized, stage_order, manager_number, proposed_stage,
       CASE WHEN proposed_stage IS NULL THEN 'UNRECOGNIZED_STAGE'
            WHEN manager_number > 2 THEN 'MORE_THAN_TWO_MANAGER_STAGES'
            ELSE 'CONTEXT_ROW' END AS diagnostic
FROM mapped_rows
WHERE (source_table, context_id) IN (
    SELECT source_table, context_id FROM mapped_rows
    WHERE proposed_stage IS NULL OR manager_number > 2
)
ORDER BY source_table, context_id, stage_order, record_id;

-- Confirm which versions succeeded/failed before selecting a recovery action.
SELECT installed_rank, version, description, success
FROM flyway_schema_history
ORDER BY installed_rank;
