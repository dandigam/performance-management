ALTER TABLE sow_milestone_positions
    ADD COLUMN seniority_id BIGINT NULL AFTER seniority;

UPDATE sow_milestone_positions position_row
JOIN lookup_values seniority_value
    ON UPPER(REPLACE(TRIM(position_row.seniority), '-', '_')) = UPPER(seniority_value.code)
JOIN lookup_types seniority_type
    ON seniority_type.id = seniority_value.lookup_type_id
   AND UPPER(seniority_type.code) = 'SENIORITY'
SET position_row.seniority_id = seniority_value.id;

ALTER TABLE sow_milestone_positions
    MODIFY seniority_id BIGINT NOT NULL,
    ADD INDEX idx_smp_seniority_id (seniority_id),
    ADD CONSTRAINT fk_smp_seniority
        FOREIGN KEY (seniority_id) REFERENCES lookup_values(id),
    DROP COLUMN seniority;

ALTER TABLE sow_resource_requirement
    ADD COLUMN seniority_id BIGINT NULL AFTER seniority;

UPDATE sow_resource_requirement requirement_row
JOIN lookup_values seniority_value
    ON UPPER(REPLACE(TRIM(requirement_row.seniority), '-', '_')) = UPPER(seniority_value.code)
JOIN lookup_types seniority_type
    ON seniority_type.id = seniority_value.lookup_type_id
   AND UPPER(seniority_type.code) = 'SENIORITY'
SET requirement_row.seniority_id = seniority_value.id;

ALTER TABLE sow_resource_requirement
    DROP INDEX uk_resource_requirement,
    MODIFY seniority_id BIGINT NOT NULL,
    ADD INDEX idx_resource_requirement_seniority_id (seniority_id),
    ADD CONSTRAINT fk_resource_requirement_seniority
        FOREIGN KEY (seniority_id) REFERENCES lookup_values(id),
    ADD CONSTRAINT uk_resource_requirement
        UNIQUE (sow_id, position_id, skill_id, seniority_id, location),
    DROP COLUMN seniority;
