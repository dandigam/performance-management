UPDATE users user_account
JOIN employees employee ON employee.id = user_account.employee_id
SET user_account.status = 'INACTIVE'
WHERE UPPER(employee.status) <> 'ACTIVE'
   OR employee.status IS NULL;