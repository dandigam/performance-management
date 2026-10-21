# Timesheet schedule changes

`POST /api/v1/employees/{employeeId}/timesheet-projects` accepts a JSON array of
assignment configurations. All assignment metadata and schedule changes in a
request execute in one transaction. Any failure rolls back the entire batch.

```json
[
  {
    "timesheetEmployeeProjectId": 123,
    "sowId": 45,
    "milestoneId": 67,
    "startDate": "2026-09-01",
    "endDate": "2026-09-30",
    "defaultHoursPerDay": null,
    "scheduleDates": [
      { "workDate": "2026-09-01", "scheduledHours": 8 },
      { "workDate": "2026-09-02", "scheduledHours": 6.5 }
    ],
    "deletedDates": [
      { "workDate": "2026-09-03" }
    ],
    "level1ApproverId": 10,
    "level2ApproverId": 20,
    "status": "ACTIVE"
  }
]
```

- `scheduleDates` inserts or updates by assignment ID and work date. Send only new
  or changed dates. Hours must be between 0 and 24, inclusive; decimals are supported.
- `deletedDates` physically deletes only matching schedule rows for that assignment.
  A missing row is a no-op, so deletion can be retried. Actual timesheet entries
  are separate records and are not deleted by this endpoint.
- Omitted dates remain unchanged, even if assignment start/end dates or default hours
  change. Unlisted dates are never generated, zeroed, deactivated, or deleted.
- Empty arrays mean no changes. To remove one day from 150 saved days, send
  `scheduleDates: []` and that single date in `deletedDates`.
- For creation, omit `timesheetEmployeeProjectId`, send the initial dates in
  `scheduleDates`, and use `deletedDates: []`.
- An existing assignment ID must belong to the URL employee and match the supplied
  SOW and milestone. Updates cannot move an assignment to a different configuration.
- Upsert dates must fall within the effective assignment/milestone range. Explicit
  deletions may reference dates outside the updated range.
- Duplicate dates or a date in both lists are rejected. Upserts to locked/submitted
  timesheet dates and combined scheduled hours above 24 per day are also rejected.
- `defaultHoursPerDay` is stored as configuration metadata; it no longer regenerates
  schedule rows. Explicit zero-hour upserts are allowed, but no zero-hour rows are
  created for unlisted dates.
- The old request field `dailyOverrides` is rejected with a migration message. It
  must not be used as a replacement list with this contract.

GET/detail, POST, and PUT responses use `scheduleDates` instead of `dailyOverrides`.
It contains all active stored schedule dates, including dates whose hours equal
the default. Each entry retains `workDate`, `scheduledHours`, and `dayType`.
Clients must read `scheduleDates`; the old response field is no longer returned.
The existing PUT endpoint applies the same date-change semantics.
