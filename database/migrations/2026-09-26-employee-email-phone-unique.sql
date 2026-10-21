-- Normalize existing values before applying uniqueness constraints.
-- This migration intentionally fails if real duplicate emails or phone numbers already exist;
-- resolve those records before rerunning it.
UPDATE employees
SET email = LOWER(TRIM(email))
WHERE email IS NOT NULL;

UPDATE employees
SET phone_number = NULLIF(TRIM(phone_number), '')
WHERE phone_number IS NOT NULL;

ALTER TABLE employees
    ADD CONSTRAINT uk_employee_email UNIQUE (email),
    ADD CONSTRAINT uk_employee_phone_number UNIQUE (phone_number);
