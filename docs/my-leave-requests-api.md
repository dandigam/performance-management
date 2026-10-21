# My leave requests

These APIs are for the logged-in employee. Send the normal authentication token; the server resolves the employee from that account. No `employeeId` is accepted in these requests.

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/api/v1/my/leave-balances?year=2026` | Get my balances for a year |
| `GET` | `/api/v1/my/leave-requests` | Get my leave requests |
| `GET` | `/api/v1/my/leave-requests/{id}` | Get one of my requests |
| `GET` | `/api/v1/my/leave-requests/schedule?leaveTypeId=4&fromDate=2026-09-21&toDate=2026-09-23` | Calculate the configured schedule and show balance |
| `POST` | `/api/v1/my/leave-requests` | Create a draft |
| `PUT` | `/api/v1/my/leave-requests/{id}` | Edit a draft |
| `POST` | `/api/v1/my/leave-requests/{id}/submit` | Submit a draft |
| `POST` | `/api/v1/my/leave-requests/{id}/cancel` | Cancel a submitted request |

## Check the schedule

Call the schedule endpoint before creating a draft. It reads positive hours from the employee's active Timesheet Setup rows for the requested dates. It sums hours from multiple configured projects on the same date. Dates without positive configured hours are omitted, including weekends and holidays unless explicitly scheduled. It also checks that the date range is within one active leave policy assignment and that the leave type is active in that policy.

Example response:

```json
{
  "employeeLeavePolicyId": 3,
  "leaveTypeId": 4,
  "leaveTypeCode": "PTO",
  "leaveTypeName": "Paid time off",
  "unit": "HOURS",
  "availableBalance": 32.00,
  "availableByYear": { "2026": 32.00 },
  "unlimited": false,
  "fromDate": "2026-09-21",
  "toDate": "2026-09-23",
  "days": [
    { "id": null, "leaveDate": "2026-09-21", "scheduledHours": 8.00, "requestedHours": 8.00 },
    { "id": null, "leaveDate": "2026-09-22", "scheduledHours": 4.00, "requestedHours": 4.00 },
    { "id": null, "leaveDate": "2026-09-23", "scheduledHours": 8.00, "requestedHours": 8.00 }
  ]
}
```

The schedule response uses full scheduled hours as a starting value for `requestedHours`. Reduce `requestedHours` for partial leave. `availableBalance` is for `fromDate`'s year; `availableByYear` includes each year in a range that crosses a year boundary. Unlimited balances return `"Unlimited"` and `unlimited: true`. Balances must first be initialized for the relevant assignment and year.

## Create or update a draft

Use the same JSON body for `POST` and `PUT`:

```json
{
  "leaveTypeId": 4,
  "fromDate": "2026-09-21",
  "toDate": "2026-09-23",
  "reason": "Personal leave",
  "notes": "Optional note",
  "days": [
    { "leaveDate": "2026-09-21", "requestedHours": 8.00 },
    { "leaveDate": "2026-09-22", "requestedHours": 2.00 },
    { "leaveDate": "2026-09-23", "requestedHours": 8.00 }
  ]
}
```

`leaveTypeId`, both dates, `reason`, and at least one requested day are required. Each requested date must be within the range and have positive configured scheduled hours. Requested hours must be greater than zero and no more than that date's scheduled hours. A date can appear only once. The server calculates `scheduledHours` and `totalHours`; it ignores any client-supplied total. `POST` returns `201 Created` with a `Location` header. `PUT` works only while the request is `DRAFT`.

Example saved response:

```json
{
  "id": 12,
  "employeeLeavePolicyId": 3,
  "leaveTypeId": 4,
  "leaveTypeCode": "PTO",
  "leaveTypeName": "Paid time off",
  "unit": "HOURS",
  "fromDate": "2026-09-21",
  "toDate": "2026-09-23",
  "totalHours": 18.00,
  "reason": "Personal leave",
  "notes": "Optional note",
  "status": "DRAFT",
  "submittedAt": null,
  "createdAt": "2026-09-19T09:00:00",
  "createdBy": 7,
  "updatedAt": "2026-09-19T09:00:00",
  "updatedBy": 7,
  "days": [
    { "id": 20, "leaveDate": "2026-09-21", "scheduledHours": 8.00, "requestedHours": 8.00 },
    { "id": 21, "leaveDate": "2026-09-22", "scheduledHours": 4.00, "requestedHours": 2.00 },
    { "id": 22, "leaveDate": "2026-09-23", "scheduledHours": 8.00, "requestedHours": 8.00 }
  ]
}
```

## Submit and cancel

`POST /api/v1/my/leave-requests/{id}/submit` needs no body. It rechecks the current assignment, policy rule, schedule, overlapping requests, and available balance. On success it changes `DRAFT` to `SUBMITTED` and sets `submittedAt`. Only drafts can be edited or submitted. Submission does **not** change the balance's `used` value; that belongs to the later approval workflow.

`POST /api/v1/my/leave-requests/{id}/cancel` needs no body. It changes only `SUBMITTED` to `CANCELLED` and retains the request and daily records. Employees cannot cancel `LEVEL1_APPROVED`, `APPROVED`, or `REJECTED` requests through this API.

Requests in `SUBMITTED`, `LEVEL1_APPROVED`, or `APPROVED` block another request on the same employee/date. `REJECTED` and `CANCELLED` requests do not block. A requested date cannot exceed its scheduled hours. Limited `HOURS` balances compare requested hours to available hours. Limited `DAYS` balances compare the sum of each day's `requestedHours / scheduledHours` to available days. Unlimited balances skip the numeric check. A request spanning two years checks each year's balance separately.

## Errors

`401` means employee authentication is missing or the account is not linked to an employee. `404` means a request ID is not owned by the logged-in employee. `400` covers invalid dates, schedule, leave type, balance, overlap, or status transition. Validation errors also return `400` with field names.
