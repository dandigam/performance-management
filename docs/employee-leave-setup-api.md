# Employee leave setup list

GET `/api/v1/employees/leave-setup?year=2026&status=ALL&search=&page=0&size=20`

Uses the same authentication rules as the existing employee leave-policy endpoints.

- `year` is required (1–9999).
- `status`: ALL (default), SET_UP, PENDING. Case-insensitive.
- `search`: optional case-insensitive literal substring of full name or employee number (`rit_id`).
- `page`: zero-based, default 0. `size`: 1–100, default 20.
- Invalid parameters return 400.

Returns `content`, `totalElements`, `totalPages`, `number`, and `size`. Each row contains
`employeeId`, `employeeNumber`, `employeeName`, `employeeLeavePolicyId`, `leavePolicyId`,
`policyName`, and `setupStatus`.

Includes all employees, even without assignments. Employee number is the stored `rit_id`,
not a generated value. Rows sort by employee name then ID. Filtering occurs before pagination.

For one row per employee, select the latest ACTIVE assignment overlapping the requested
calendar year (effective-from descending, then ID descending). Earlier assignments in the
same year are not summarized. Without an eligible assignment, policy fields are null.

SET_UP means the selected assignment's policy is active and overlaps the year, has at least
one active rule, and every active rule has an ACTIVE balance for that employee, assignment,
leave type and year. Otherwise the row is PENDING. Partial initialization, inactive balances,
and balances from other years do not qualify. Zero available balance still counts as SET_UP.
This is policy/balance initialization status, not validation of approvers or work schedules.

PENDING rows can still contain policy IDs: assignment alone does not initialize balances.
Use POST `/api/v1/employees/{employeeId}/leave-balances/initialize` with
`{"employeeLeavePolicyId":2,"balanceYear":2026}` to initialize an existing assignment.
This GET does not assign policies or create balances.
