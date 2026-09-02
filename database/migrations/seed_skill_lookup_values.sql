-- Seed commonly used skills under the existing SKILL lookup type.
-- Existing values are preserved and duplicate codes are not inserted.
INSERT INTO lookup_values (
    lookup_type_id,
    code,
    name,
    description,
    display_order,
    is_active,
    created_on,
    updated_on
)
SELECT
    skill_type.id,
    skills.code,
    skills.name,
    NULL,
    skills.display_order,
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM lookup_types skill_type
JOIN (
    SELECT 'JAVA' code, 'Java' name, 1 display_order
    UNION ALL SELECT 'SPRING_BOOT', 'Spring Boot', 2
    UNION ALL SELECT 'DEVOPS', 'DevOps', 3
    UNION ALL SELECT 'AWS', 'AWS', 4
    UNION ALL SELECT 'ANGULAR', 'Angular', 5
    UNION ALL SELECT 'REACT', 'React', 6
    UNION ALL SELECT 'JAVASCRIPT', 'JavaScript', 7
    UNION ALL SELECT 'TYPESCRIPT', 'TypeScript', 8
    UNION ALL SELECT 'PYTHON', 'Python', 9
    UNION ALL SELECT 'NODE_JS', 'Node.js', 10
    UNION ALL SELECT 'SQL', 'SQL', 11
    UNION ALL SELECT 'DOCKER', 'Docker', 12
    UNION ALL SELECT 'KUBERNETES', 'Kubernetes', 13
    UNION ALL SELECT 'AZURE', 'Azure', 14
) skills
WHERE UPPER(skill_type.code) = 'SKILL'
  AND NOT EXISTS (
      SELECT 1
      FROM lookup_values existing
      WHERE existing.lookup_type_id = skill_type.id
        AND UPPER(existing.code) = skills.code
  );
