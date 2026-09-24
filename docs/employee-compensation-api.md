# Employee compensation

`compensationDetails` is optional in employee create and update requests. An empty
object is ignored. If provided with values, `payType`, `currency`, and
`effectiveDate` are required. Dates use `YYYY-MM-DD`; currency is a three-letter
code. Amounts must be greater than zero and have at most two decimal places.

| Pay type | Amount field |
| --- | --- |
| `W2_SALARY` (also accepts `W2 Salary` or legacy `SALARY`) | `annualSalary` |
| `W2_HOURLY` (also accepts `W2 Hourly` or legacy `HOURLY`) | `hourlyRate` |
| `1099` | `hourlyRate` |
| `C2C` | `hourlyRate` |
| `CONTRACT` | `hourlyRate` |

Send only the amount field for the chosen pay type. For example:

```json
{"compensationDetails":{"payType":"W2_SALARY","annualSalary":45000,"currency":"USD","effectiveDate":"2026-09-01"}}
```

Employee detail responses and finance history return both `annualSalary` and
`hourlyRate`; the unused field is null. Finance history's `amount` contains the
applicable value for the pay type.

Existing databases need the following schema change before salary records can
be saved. Hibernate `ddl-auto=update` may add `annual_salary`, but does not
reliably remove the existing `NOT NULL` constraint from `hourly_rate`.

```sql
ALTER TABLE employee_compensations
    MODIFY COLUMN hourly_rate DECIMAL(12,2) NULL,
    ADD COLUMN annual_salary DECIMAL(14,2) NULL;
```

If `annual_salary` already exists, omit the `ADD COLUMN` clause.

Existing `HOURLY` and `SALARY` values should be converted to the UI pay types:

```sql
UPDATE employee_compensations
SET pay_type = 'W2_HOURLY'
WHERE UPPER(TRIM(pay_type)) = 'HOURLY';

UPDATE employee_compensations
SET annual_salary = COALESCE(annual_salary, hourly_rate),
    hourly_rate = NULL,
    pay_type = 'W2_SALARY'
WHERE UPPER(TRIM(pay_type)) = 'SALARY';
```

Review any legacy `SALARY` amounts before applying the second update, since
the old schema held them in a column named `hourly_rate`. The API maps legacy
pay type names to canonical names when reading rows that have not yet been
migrated.
