-- Manual reference script. ProductionReferenceDataInitializer seeds these at startup.
-- Keep existing IDs and role settings. The catalog contains code/name only, no Includes column.
START TRANSACTION;

-- Rename the old feature in place when finance does not already exist.
UPDATE features old_feature
LEFT JOIN features current_feature ON current_feature.code = 'finance'
SET old_feature.code = 'finance', old_feature.name = 'Finance'
WHERE old_feature.code = 'invoices' AND current_feature.id IS NULL;

-- If both codes exist, migrate only missing role settings; explicit finance flags win.
INSERT INTO role_feature_flags (feature_id, role_id, enabled)
SELECT finance.id, old_flag.role_id, old_flag.enabled
FROM role_feature_flags old_flag
JOIN features old_feature ON old_feature.id = old_flag.feature_id AND old_feature.code = 'invoices'
JOIN features finance ON finance.code = 'finance'
LEFT JOIN role_feature_flags existing ON existing.feature_id = finance.id AND existing.role_id = old_flag.role_id
WHERE existing.id IS NULL;

DELETE flag FROM role_feature_flags flag
JOIN features old_feature ON old_feature.id = flag.feature_id
WHERE old_feature.code = 'invoices';
DELETE FROM features WHERE code = 'invoices';

INSERT INTO features (code, name) VALUES
('people', 'People'),
('dashboard', 'Dashboard'),
('performance', 'Performance'),
('clients', 'Clients'),
('projects', 'Projects & SOWs'),
('timesheets', 'Timesheets'),
('finance', 'Finance'),
('vendors', 'Vendors'),
('leave_calendar', 'Leave & Calendar'),
('reports', 'Reports')
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- Explicitly store false for newly introduced mappings; preserve existing true/false values.
INSERT INTO role_feature_flags (feature_id, role_id, enabled)
SELECT feature.id, role.id, FALSE
FROM features feature
CROSS JOIN lookup_values role
JOIN lookup_types type ON type.id = role.lookup_type_id
WHERE feature.code IN ('people', 'performance', 'clients', 'projects', 'timesheets',
                       'finance', 'vendors', 'leave_calendar', 'reports', 'dashboard')
  AND type.code = 'SYSTEM_ROLE' AND type.is_active = TRUE AND role.is_active = TRUE
  AND NOT EXISTS (SELECT 1 FROM role_feature_flags existing
                  WHERE existing.feature_id = feature.id AND existing.role_id = role.id);

COMMIT;
