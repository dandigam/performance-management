# Employee leave balances

Base path: `/api/v1/employees/{employeeId}/leave-balances`. JSON requests and responses use the application's existing `/api/**` authentication rules.

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/api/v1/employees/{employeeId}/leave-balances/initialize` | Create balances from an active policy assignment |
| `GET` | `/api/v1/employees/{employeeId}/leave-balances?year=2026` | List employee balances for a year |
| `GET` | `/api/v1/employees/{employeeId}/leave-balances/{balanceId}` | Get one balance |
| `POST` | `/api/v1/employees/{employeeId}/leave-balances/{balanceId}/adjustments` | Add an adjustment |
| `GET` | `/api/v1/employees/{employeeId}/leave-balances/{balanceId}/adjustments` | Get adjustment history |

## Initialize

```json
{ "employeeLeavePolicyId": 3, "balanceYear": 2026 }
```

The assignment must belong to the employee, have `ACTIVE` status, and overlap the requested year. Initialization creates one balance for each active policy rule. It copies the rule's entitlement directly; no prorating or monthly accrual is applied. Repeating initialization leaves existing balance rows unchanged. A database unique constraint prevents duplicate rows for the same assignment, leave type, and year. The response is an array of balances for that assignment and year.

## Balance response

```json
{
  "id": 12,
  "employeeId": 1,
  "employeeLeavePolicyId": 3,
  "leaveTypeId": 4,
  "leaveTypeCode": "PTO",
  "leaveTypeName": "Paid time off",
  "unit": "HOURS",
  "balanceYear": 2026,
  "openingBalance": 0.00,
  "entitled": 120.00,
  "totalAdjustments": 5.00,
  "used": 10.00,
  "available": 115.00,
  "unlimited": false,
  "status": "ACTIVE",
  "createdAt": "2026-01-01T09:00:00",
  "createdBy": 7,
  "updatedAt": "2026-01-01T09:00:00",
  "updatedBy": 7
}
```

`available = openingBalance + entitled + totalAdjustments - used`. `totalAdjustments` is signed: `ADD` increases it and `DEDUCT` decreases it. It is calculated from adjustment history and is not stored on the balance. For unlimited leave, `entitled` is `null`, `unlimited` is `true`, and `available` is the string `"Unlimited"`. Units are `HOURS` or `DAYS` as defined by the leave type. Audit user IDs may be `null` when no auditor is available.

## Add an adjustment

```json
{
  "adjustmentType": "ADD",
  "amount": 5.00,
  "reason": "Manual correction",
  "notes": "Approved by HR",
  "adjustmentDate": "2026-03-01"
}
```

`adjustmentType` is `ADD` or `DEDUCT`; `amount` must be positive with at most two decimal places. `reason` and `adjustmentDate` are required. `notes` is optional. The API returns `201 Created` with the saved adjustment, including `id`, `employeeLeaveBalanceId`, `createdAt`, and `createdBy`. Adjustments are retained as separate records; adding one does not overwrite the balance row. The history endpoint returns adjustments by date and ID ascending.

Invalid requests return `400`. Missing employees, assignments, or balances return `404`. Initialization with an inactive assignment or a year outside its period returns `400`.
