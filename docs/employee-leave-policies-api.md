# Employee leave policy assignments

Base path: `/api/v1/employees/{employeeId}/leave-policies`. Requests and responses use JSON. Dates use `YYYY-MM-DD`. Authentication follows the application's existing `/api/**` rules.

| Method | Path | Purpose | Success |
| --- | --- | --- | --- |
| `POST` | `/api/v1/employees/{employeeId}/leave-policies` | Assign a policy | `201 Created`, with `Location` header |
| `PUT` | `/api/v1/employees/{employeeId}/leave-policies/{assignmentId}` | Update an assignment | `200 OK` |
| `GET` | `/api/v1/employees/{employeeId}/leave-policies` | List assignment history | `200 OK` |
| `GET` | `/api/v1/employees/{employeeId}/leave-policies/current` | Get today's active assignment | `200 OK` |
| `PATCH` | `/api/v1/employees/{employeeId}/leave-policies/{assignmentId}/status` | Activate or deactivate | `200 OK` |

## Assign or update

`POST` and `PUT` use the same body:

```json
{
  "leavePolicyId": 10,
  "effectiveFrom": "2026-01-01",
  "effectiveTo": "2026-06-30",
  "level1ApproverId": 40,
  "level2ApproverId": 55,
  "status": "ACTIVE"
}
```

`leavePolicyId`, `effectiveFrom`, and `level1ApproverId` are required. `level2ApproverId`, `effectiveTo`, and `status` are optional. Omit `status` to create an `ACTIVE` assignment or to retain the current status on update. Approvers must be active employees. The employee cannot approve their own leave, and Level 1 and Level 2 must differ. `effectiveTo: null` means no end date. An end date must be on or after the start date. The entire assignment period must fit within the selected policy's effective period. If the policy has an end date, the assignment must also have an end date no later than it.

A new assignment requires an `ACTIVE` leave policy. Changing an assignment to a different policy also requires that policy to be `ACTIVE`. Existing assignments retain their status when updated.

`POST` defaults to an `ACTIVE` assignment. It returns `201 Created` and a `Location` header such as `/api/v1/employees/5/leave-policies/42`.

## Response

The single assignment endpoints return this shape. The list endpoint returns an array of these objects, ordered by `effectiveFrom` descending and then ID descending.

```json
{
  "id": 42,
  "employeeId": 5,
  "leavePolicyId": 10,
  "policyName": "India full-time policy",
  "effectiveFrom": "2026-01-01",
  "effectiveTo": "2026-06-30",
  "status": "ACTIVE",
  "level1ApproverId": 40,
  "level1ApproverName": "Charan Patel",
  "level2ApproverId": 55,
  "level2ApproverName": "Robert Singh",
  "createdAt": "2026-01-01T09:00:00",
  "createdBy": 7,
  "updatedAt": "2026-01-01T09:00:00",
  "updatedBy": 7
}
```

Audit fields are read-only and may be `null` if no auditor is available. `GET /current` uses today's date and returns an assignment whose status is `ACTIVE` and whose date range includes today.

Submitted leave requests capture these two approvers from the active assignment. Editing the assignment later changes approvers for future submissions only. Timesheet approvers are independent. Existing assignments need their Level 1 approver populated before they can be used for new leave submissions; see `database/migrations/2026-09-19-leave-policy-approvers.sql`.

## Change status

`PATCH /api/v1/employees/{employeeId}/leave-policies/{assignmentId}/status`:

```json
{ "status": "INACTIVE" }
```

Use `ACTIVE` to reactivate. Reactivation requires the referenced policy to be `ACTIVE` and the assignment period to have no overlap with another active assignment. Deactivation preserves the row and its history. There is no delete endpoint.

## Period and overlap rules

An employee can have multiple assignments over time. Active assignment periods must not overlap, even when they reference different policies. Endpoints are inclusive: an assignment ending on June 30 and another starting July 1 are valid; two assignments that both include June 30 conflict. An open-ended active assignment overlaps every later period. Inactive assignments remain in history and do not block another assignment.

## Errors

| Status | Cause | Example |
| --- | --- | --- |
| `400 Bad Request` | Invalid dates, period outside the policy, inactive policy, or overlapping active periods | `{ "error": "An active leave policy assignment overlaps this period." }` |
| `404 Not Found` | Employee, policy, or assignment missing; assignment belongs to another employee; no current active assignment | `{ "error": "No current active leave policy assignment for employee: 5" }` |

Missing request fields return `400` with a field-specific validation message. `GET` for an employee with no assignment history returns `[]`.
