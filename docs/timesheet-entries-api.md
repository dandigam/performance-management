# Save actual timesheet hours

`POST /api/timesheets/{timesheetId}/entries` creates and updates entries in an existing setup-linked DRAFT week. The same endpoint handles subsequent saves; no PUT is required.

```json
{
  "entries": [
    {"workDate": "2026-09-14", "entryType": "REGULAR", "hours": 8},
    {"workDate": "2026-09-15", "entryType": "REGULAR", "hours": 8},
    {"workDate": "2026-09-16", "entryType": "HOLIDAY", "hours": 8, "holidayId": 9},
    {"workDate": "2026-09-17", "entryType": "REGULAR", "hours": 8},
    {"workDate": "2026-09-18", "entryType": "REGULAR", "hours": 8}
  ]
}
```

Use a real configured active holiday ID matching the date. The existing calendar uses Sunday–Saturday weeks (September 13–19 for this example).

Entries match by workDate and entryType within the supplied timesheet. Repeated saves update existing rows without duplicating them. Omitted entries are preserved. To clear hours, save zero for that date/type; when changing type, explicitly zero the old type. Duplicate date/type pairs in one request are rejected.

Response: HTTP 200 with timesheetId, status, regularHours, holidayHours, leaveHours, totalHours and entries including entryId. The sample totals are 32 regular, 8 holiday, 0 leave and 40 total. Omit status (or send null or DRAFT) to save a draft. Send top-level `"status":"SUBMITTED"` alongside entries to save and submit in the same transaction: the response status becomes SUBMITTED and submitted_at is recorded. Other statuses are rejected. Submitted timesheets cannot be edited or submitted again through this endpoint. Submission also creates two PENDING rows in timesheet_approvals, one for each configured setup approver (approval levels 1 and 2), in the same transaction. Both approvers must be configured before submission. Draft saves create no approval records. Approval actions are handled separately; approve/reject endpoints are not yet implemented. Previously submitted timesheets are not backfilled by this endpoint.

No sowId or employeeId is needed: the weekly header identifies the setup and employee, and SOW is derived from the setup (null for internal work). holidayId is required for HOLIDAY and forbidden otherwise. LEAVE hours can be saved with leaveId null; non-null leaveId is rejected until approved leave requests are implemented.

Dates must fall in the week and effective setup dates and have an active, unlocked scheduled day. Cancelled days and non-DRAFT or previously submitted/approved headers cannot be edited. Hours must be 0–24 with at most two decimal places; combined types cannot exceed 24 per date in this timesheet. Changes and totals are saved in one transaction with a header write lock. Authentication follows the existing API security configuration.

No database migration is needed for this endpoint. The unused jobId compatibility property is nonpersistent to match the provided table without a job_id column.
