# Employee timesheet tabs

`GET /api/timesheets?employeeId=3&status=DRAFT`

The response remains an array of existing `TimesheetSummaryResponse` objects,
newest week first. No matches returns `[]`. Status is case-insensitive; omitted
status defaults to ALL. Unknown status returns 400; unknown employee returns 404.

| UI tab | Request status | Included stored statuses |
| --- | --- | --- |
| New / Draft | NEW or DRAFT | DRAFT |
| Pending | PENDING | SUBMITTED, LEVEL1_APPROVED |
| Rejected | REJECTED (or REJECT) | REJECTED |
| Approved | APPROVED | APPROVED |
| All | ALL | All statuses |

Exact SUBMITTED and LEVEL1_APPROVED filters are also supported. Draft includes
all older unsubmitted headers and the current week's draft, not just last week.
Rejected records appear in their separate tab. Every filter excludes future weeks
in the database query, including ALL and requests without an employeeId. The
existing optional employeeId behavior is retained for the all-employee list.

For a selected record:
`GET /api/timesheets/week?employeeId=3&weekStart=2026-09-06&timesheetId=123`

The timesheetId is optional; without it the API finds the employee/week header.
weekStart must be Sunday. Requests for future weeks return 400. ID/employee/week
mismatches return 404. Weeks use the backend clock and Sunday–Saturday boundaries.

The obsolete /load and manual generation endpoints were removed. Use the status
list and /week endpoints. Week projects include scheduleDates and milestone details.

Week details no longer query the holiday calendar. Fetch holiday metadata through
the holiday API. The `days` array is removed; the UI generates date columns from
`weekStart` and `weekEnd`. Project `entries` retain saved daily entry
details, including saved holiday references; `scheduleDates` describe the schedule.

Future schedule setup and weekly-header generation remain supported; these are
write operations, not fetch endpoints. Only saved weekly headers appear in the tab list. This change does not modify the UI.

Each timesheet includes `auditLog`, an array of recorded actions, newest timestamp
first. Omitted or blank status (including `?employeeId=2&status`) means ALL.

Example auditLog item:
```json
{"action":"APPROVED","stage":"Primary","approvalLevel":1,"employeeId":3,"employeeName":"Primary Approver","actionAt":"2026-09-08T14:20:00","comments":"Checked hours"}
```

Submission uses action SUBMITTED, stage Submitted, approvalLevel null, the
sheet's employee and submittedAt. Approval actions use APPROVED or REJECTED,
stage Primary (level 1) or Secondary (level 2), the recorded approver, actionAt
and comments. Pending rows are excluded; a draft without actions returns [].
Timestamps are stored local date-times without a timezone offset; the API does
not invent timestamps for legacy approval rows (null dates sort last).

This is a timeline derived from the existing submission and approval records,
not a separate immutable history of edits or resubmission cycles. Names reflect
current employee names. Approve/reject endpoints remain to be implemented;
completed approval records appear here when those actions are persisted.
