ALTER TABLE sow_features
    ADD COLUMN milestone_id BIGINT NULL AFTER sow_id,
    ADD COLUMN description VARCHAR(1000) NULL AFTER feature_name;

UPDATE sow_features feature
LEFT JOIN sow_milestones milestone ON milestone.id = feature.milestone_id
SET feature.milestone_id = NULL
WHERE feature.milestone_id IS NOT NULL
  AND milestone.id IS NULL;

ALTER TABLE sow_features
    ADD CONSTRAINT fk_sow_features_milestone
        FOREIGN KEY (milestone_id) REFERENCES sow_milestones(id)
        ON DELETE SET NULL;
