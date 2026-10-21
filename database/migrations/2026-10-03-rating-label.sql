-- Create the type only; lookup values are managed by the user.
INSERT INTO lookup_types (code, name, description, is_active)
SELECT 'RATING_LABEL', 'Rating Label', 'Labels for performance ratings', TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM lookup_types WHERE UPPER(code) = 'RATING_LABEL'
);
