ALTER TABLE employees
    ADD COLUMN IF NOT EXISTS gender VARCHAR(30) NULL AFTER phone_number,
    ADD COLUMN IF NOT EXISTS date_of_birth DATE NULL AFTER gender;
