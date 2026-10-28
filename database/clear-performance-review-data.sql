-- Clears performance execution data only. Select the intended database first.
-- Preserves employees, organization hierarchy, lookup values, cycles, scope,
-- sections, questions, assessors, timelines, rating scales, and cycle statuses.
-- Pause application writes and notification dispatch while running this script.
START TRANSACTION;
DELETE FROM email_notifications
WHERE employee_review_id IS NOT NULL
   OR event_type IN ('CYCLE_PUBLISHED', 'ASSESSMENT_READY', 'ASSESSMENT_REOPENED', 'RESULT_PUBLISHED');
DELETE FROM user_notifications WHERE related_record_type = 'EMPLOYEE_REVIEW';
DELETE FROM final_ratings;
DELETE FROM employee_review_answers;
DELETE FROM employee_review_assessments;
DELETE FROM employee_reviews;
COMMIT;

SELECT 'employee_reviews' AS table_name, COUNT(*) AS remaining FROM employee_reviews
UNION ALL SELECT 'employee_review_assessments', COUNT(*) FROM employee_review_assessments
UNION ALL SELECT 'employee_review_answers', COUNT(*) FROM employee_review_answers
UNION ALL SELECT 'final_ratings', COUNT(*) FROM final_ratings;

