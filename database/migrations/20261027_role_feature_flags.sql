-- Manual reference script. ProductionReferenceDataInitializer seeds these at startup.
CREATE TABLE IF NOT EXISTS features (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(60) NOT NULL,
    name VARCHAR(100) NOT NULL,
    CONSTRAINT uk_features_code UNIQUE (code)
);

CREATE TABLE IF NOT EXISTS role_feature_flags (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    feature_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_role_feature_feature FOREIGN KEY (feature_id) REFERENCES features(id),
    CONSTRAINT fk_role_feature_role FOREIGN KEY (role_id) REFERENCES lookup_values(id),
    CONSTRAINT uk_role_feature UNIQUE (feature_id, role_id)
);

INSERT INTO features (code, name)
SELECT seed.code, seed.name FROM (
    SELECT 'people' AS code, 'People' AS name
    UNION ALL SELECT 'dashboard', 'Dashboard'
    UNION ALL SELECT 'performance', 'Performance'
    UNION ALL SELECT 'clients', 'Clients'
    UNION ALL SELECT 'projects', 'Projects & SOWs'
    UNION ALL SELECT 'timesheets', 'Timesheets'
    UNION ALL SELECT 'finance', 'Finance'
    UNION ALL SELECT 'vendors', 'Vendors'
    UNION ALL SELECT 'leave_calendar', 'Leave & Calendar'
    UNION ALL SELECT 'reports', 'Reports'
) seed WHERE NOT EXISTS (SELECT 1 FROM features existing WHERE existing.code = seed.code);

-- Seed the example settings. Other mappings default to false.
-- Preserve existing values when this script is rerun.
INSERT INTO role_feature_flags (feature_id, role_id, enabled)
SELECT feature.id, role.id,
       CASE WHEN (feature.code = 'people' AND role.code IN ('EMPLOYEE', 'HR'))
                  OR (feature.code = 'performance' AND role.code = 'EMPLOYEE')
            THEN TRUE ELSE FALSE END
FROM features feature
CROSS JOIN lookup_values role
JOIN lookup_types type ON type.id = role.lookup_type_id
WHERE type.code = 'SYSTEM_ROLE' AND type.is_active = TRUE AND role.is_active = TRUE
  AND NOT EXISTS (SELECT 1 FROM role_feature_flags existing
                  WHERE existing.feature_id = feature.id AND existing.role_id = role.id)
  -- Let the next migration copy legacy invoice flags before creating finance defaults.
  -- An existing explicit finance mapping still takes precedence.
  AND NOT (feature.code = 'finance' AND EXISTS (
      SELECT 1 FROM role_feature_flags legacy_flag
      JOIN features legacy_feature ON legacy_feature.id = legacy_flag.feature_id
      WHERE legacy_feature.code = 'invoices' AND legacy_flag.role_id = role.id));
