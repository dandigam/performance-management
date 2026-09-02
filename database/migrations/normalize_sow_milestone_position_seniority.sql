UPDATE sow_milestone_positions
SET seniority = UPPER(TRIM(seniority))
WHERE seniority IS NOT NULL;
