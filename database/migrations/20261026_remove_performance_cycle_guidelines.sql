-- Removes obsolete performance-cycle guideline file columns.
-- Java entities, requests, responses, services, and mappers no longer use these fields.
ALTER TABLE performance_cycles
    DROP COLUMN employee_guideline_file,
    DROP COLUMN manager_guideline_file;
