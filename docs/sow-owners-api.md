# SOW Delivery Owner and Technical Lead

## My managed SOWs

`GET /api/v1/sows/my-managed` derives the employee from the authenticated account;
there is no employee ID parameter. An active user linked to an employee is required,
including when the development authentication bypass is enabled (otherwise 401).
Onboarding-only accounts are denied by the existing authentication filter (403).

```json
{
  "managedSows": [
    {
      "sowId": 101,
      "sowName": "2026 FOCUS Conversion 1.0",
      "status": "ACTIVE",
      "startDate": "2026-01-01",
      "endDate": "2026-10-31",
      "responsibilities": ["TECHNICAL_LEAD"],
      "canViewSow": true
    }
  ]
}
```

Matches either current ownership field in `sows`, across all statuses, ordered by
SOW ID. Each SOW appears once; when both fields match, responsibilities contains
`DELIVERY_OWNER` followed by `TECHNICAL_LEAD`. No matches returns `{"managedSows":[]}`.
Historical owners in `sow_owner_history` do not grant membership in this list.
The database query is restricted to the signed-in employee, including for admins.

`canViewSow` reflects the current server policy: authenticated full-portal users
can access SOW details, so it is currently true for every returned row. Ownership
does not introduce a separate detail-access restriction. The flag is not an
authorization token; detail APIs continue enforcing their server-side access rules.
No additional table, migration, or scheduled job is required for this endpoint.

## Assign owners

SOW create/update requests (`POST /api/v1/sows`, `PUT /api/v1/sows/{id}`)
accept two optional RIT employee selections:

```json
{
  "deliveryOwnerEmployeeId": 12,
  "technicalLeadEmployeeId": 34
}
```

Include these fields alongside the existing required SOW fields. IDs reference
`employees`, not `users` or CSX contacts. Unknown IDs return 404. Null or omitted
values clear the corresponding selection during a full SOW update.

SOW detail/list/summary/create/update responses include both IDs and
`deliveryOwnerEmployeeName` / `technicalLeadEmployeeName`.

These selections belong only to the SOW. They do not change assignment-level
manager/lead fields, approval routing, or notification recipients.

The columns are `sows.delivery_owner_employee_id` and
`sows.technical_lead_employee_id`. Apply
[the migration](../database/migrations/2026-10-04-sow-owners.sql) once when Hibernate
automatic schema updates are disabled. Existing records start with null selections.

## Ownership history

`sows` always contains the latest saved owners. Each changed role adds a row to
`sow_owner_history` in the same transaction. Initial assignments and clearing an
owner are recorded too. Saving the same employee again creates no history row.
Employee IDs and names are stored as snapshots, so later name changes do not rewrite
the history. New selections must reference active employees; retaining an unchanged
inactive owner is allowed until a replacement is selected.

Use `PUT /api/v1/sows/{sowId}/owners` to replace both owner selections without
resubmitting the rest of the SOW:

```json
{
  "deliveryOwnerEmployeeId": 12,
  "technicalLeadEmployeeId": 56,
  "effectiveDate": "2026-10-04",
  "reason": "Previous technical lead left the company"
}
```

Send both current IDs, changing only the desired selection. Null or omitted owner
IDs clear that selection. `effectiveDate` is required and cannot be in the future;
`reason` is optional, up to 2000 characters. The response is the updated `SowResponse`.
The change takes effect immediately; the effective date is an audit annotation,
not a scheduled change or a reconstruction of past assignments.

The full create/update endpoints also record owner history. They accept optional
`ownerChangeEffectiveDate` (defaults to today) and `ownerChangeReason` (up to 2000
characters). Existing owner-ID fields keep their original behavior.

`GET /api/v1/sows/{sowId}/owner-history` returns an array ordered by `changedAt`, then
`id`, oldest first. Unknown SOW IDs return 404. Each row contains:

- `id`, `sowId`, `role` (`DELIVERY_OWNER` or `TECHNICAL_LEAD`)
- `previousEmployeeId`, `previousEmployeeName`, `employeeId`, `employeeName`
- `effectiveDate`, `reason`, `changedAt`, `changedBy`, `baseline`

`changedBy` comes from the signed-in user, never the request. It may be null for a
baseline or an unauthenticated development-bypass operation, matching status history.
The endpoints follow the existing SOW authentication rules. There are no history
edit/delete endpoints. Ownership changes serialize using the same SOW row lock as
status updates. They do not change the SOW status or assignment approval routing.

Apply [the owner history migration](../database/migrations/2026-10-04-sow-owner-history.sql)
after the owner-columns migration. It captures existing assigned owners with
`baseline=true`, unknown `effectiveDate` and no attributed historical actor. Re-running
it does not add a baseline where role history already exists. With Hibernate-only
schema creation, the first owner change captures the prior owner as a baseline.
History uses snapshot identifiers without cascading foreign keys and survives SOW
deletion in the database; the endpoint returns 404 once the SOW has been deleted.
