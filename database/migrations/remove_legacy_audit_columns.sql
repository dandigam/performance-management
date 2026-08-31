-- Preserve existing audit timestamps before removing columns superseded by BaseEntity.
UPDATE bank_account SET created_on = COALESCE(created_on, created_at), updated_on = COALESCE(updated_on, updated_at);
ALTER TABLE bank_account DROP COLUMN created_at, DROP COLUMN updated_at;

UPDATE clients SET created_on = COALESCE(created_on, created_date), updated_on = COALESCE(updated_on, updated_date);
ALTER TABLE clients DROP COLUMN created_date, DROP COLUMN updated_date;

UPDATE csx_employees SET created_on = COALESCE(created_on, created_date), updated_on = COALESCE(updated_on, updated_date);
ALTER TABLE csx_employees DROP COLUMN created_date, DROP COLUMN updated_date;

UPDATE email_notifications SET created_on = COALESCE(created_on, created_date);
ALTER TABLE email_notifications DROP COLUMN created_date;

UPDATE employee_assignments SET created_on = COALESCE(created_on, created_date), updated_on = COALESCE(updated_on, updated_date);
ALTER TABLE employee_assignments DROP COLUMN created_date, DROP COLUMN updated_date;

UPDATE employee_compensations SET created_on = COALESCE(created_on, created_at), updated_on = COALESCE(updated_on, updated_at);
ALTER TABLE employee_compensations DROP COLUMN created_at, DROP COLUMN updated_at;

UPDATE employee_professional_profiles SET created_on = COALESCE(created_on, created_at), updated_on = COALESCE(updated_on, updated_at);
ALTER TABLE employee_professional_profiles DROP COLUMN created_at, DROP COLUMN updated_at;

UPDATE employee_review_answers SET created_on = COALESCE(created_on, created_date), updated_on = COALESCE(updated_on, updated_date);
ALTER TABLE employee_review_answers DROP COLUMN created_date, DROP COLUMN updated_date;

UPDATE employee_review_assessments SET created_on = COALESCE(created_on, created_date), updated_on = COALESCE(updated_on, updated_date);
ALTER TABLE employee_review_assessments DROP COLUMN created_date, DROP COLUMN updated_date;

UPDATE employee_reviews SET created_on = COALESCE(created_on, created_date), updated_on = COALESCE(updated_on, updated_date);
ALTER TABLE employee_reviews DROP COLUMN created_date, DROP COLUMN updated_date;

UPDATE employee_roles SET created_on = COALESCE(created_on, created_date), updated_on = COALESCE(updated_on, updated_date);
ALTER TABLE employee_roles DROP COLUMN created_date, DROP COLUMN updated_date;

UPDATE employees SET created_on = COALESCE(created_on, created_date), updated_on = COALESCE(updated_on, updated_date);
ALTER TABLE employees DROP COLUMN created_date, DROP COLUMN updated_date;

UPDATE lookup_types SET created_on = COALESCE(created_on, created_date);
ALTER TABLE lookup_types DROP COLUMN created_date;

UPDATE performance_cycle_assessors SET created_on = COALESCE(created_on, created_date), updated_on = COALESCE(updated_on, updated_date);
ALTER TABLE performance_cycle_assessors DROP COLUMN created_date, DROP COLUMN updated_date;

UPDATE performance_cycle_questions SET created_on = COALESCE(created_on, created_date), updated_on = COALESCE(updated_on, updated_date);
ALTER TABLE performance_cycle_questions DROP COLUMN created_date, DROP COLUMN updated_date;

UPDATE performance_cycle_rating_scales SET created_on = COALESCE(created_on, created_date), updated_on = COALESCE(updated_on, updated_date);
ALTER TABLE performance_cycle_rating_scales DROP COLUMN created_date, DROP COLUMN updated_date;

UPDATE performance_cycle_sections SET created_on = COALESCE(created_on, created_date), updated_on = COALESCE(updated_on, updated_date);
ALTER TABLE performance_cycle_sections DROP COLUMN created_date, DROP COLUMN updated_date;

UPDATE performance_cycle_timelines SET created_on = COALESCE(created_on, created_date), updated_on = COALESCE(updated_on, updated_date);
ALTER TABLE performance_cycle_timelines DROP COLUMN created_date, DROP COLUMN updated_date;

UPDATE performance_cycles SET created_on = COALESCE(created_on, created_date), updated_on = COALESCE(updated_on, updated_date);
ALTER TABLE performance_cycles DROP COLUMN created_date, DROP COLUMN updated_date;

UPDATE rate_cards SET created_on = COALESCE(created_on, created_date), updated_on = COALESCE(updated_on, updated_date);
ALTER TABLE rate_cards DROP COLUMN created_date, DROP COLUMN updated_date;

UPDATE refresh_tokens SET created_on = COALESCE(created_on, created_at);
ALTER TABLE refresh_tokens DROP COLUMN created_at;

UPDATE sow_features SET created_on = COALESCE(created_on, created_date), updated_on = COALESCE(updated_on, updated_date);
ALTER TABLE sow_features DROP COLUMN created_date, DROP COLUMN updated_date;

UPDATE sow_invoices SET created_on = COALESCE(created_on, created_date), updated_on = COALESCE(updated_on, updated_date);
ALTER TABLE sow_invoices DROP COLUMN created_date, DROP COLUMN updated_date;

UPDATE sow_milestones SET created_on = COALESCE(created_on, created_date), updated_on = COALESCE(updated_on, updated_date);
ALTER TABLE sow_milestones DROP COLUMN created_date, DROP COLUMN updated_date;

UPDATE vendor_invoices SET created_on = COALESCE(created_on, created_at), updated_on = COALESCE(updated_on, updated_at);
ALTER TABLE vendor_invoices DROP COLUMN created_at, DROP COLUMN updated_at;

UPDATE vendors SET created_on = COALESCE(created_on, created_date), updated_on = COALESCE(updated_on, updated_date);
ALTER TABLE vendors DROP COLUMN created_date, DROP COLUMN updated_date;

UPDATE work_orders SET created_on = COALESCE(created_on, created_at), updated_on = COALESCE(updated_on, updated_at);
ALTER TABLE work_orders DROP COLUMN created_at, DROP COLUMN updated_at;
