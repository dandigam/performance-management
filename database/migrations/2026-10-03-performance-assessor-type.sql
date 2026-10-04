-- Create the type only; lookup values are managed by the user.
INSERT INTO lookup_types (code, name, description, is_active)
SELECT 'PERFORMANCE_ASSESSOR_TYPE', 'Performance Assessor Type',
       'Assessor types for performance assessments', TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM lookup_types WHERE UPPER(code) = 'PERFORMANCE_ASSESSOR_TYPE'
);
