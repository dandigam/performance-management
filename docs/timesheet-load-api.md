# Employee timesheet APIs

Use `GET /api/timesheets?employeeId=3&status=DRAFT` for status tabs and
`GET /api/timesheets/week?employeeId=3&weekStart=2026-09-06&timesheetId=123`
for details. Future weeks are excluded. See timesheet-status-tabs-api.md for filters.
Week detail projects include milestoneId, milestoneName, and scheduleDates alongside
existing fields. Dates come from active stored schedules for the selected week.
Actual entries retain their existing SOW-based association.

Removed endpoints: POST /api/timesheets/generate,
POST /api/timesheets/generate-previous-dates-timesheets, GET /api/timesheets/load.
Clients must use the list and week endpoints instead.

Weekly headers are created by TimesheetGenerationService during schedule setup,
shared across assignments by employee and Sunday week start. Existing headers are
preserved; new headers are DRAFT with zero totals. Creation and empty-draft cleanup
participate in the setup transaction. Cleanup checks explicit deletedDates only,
retaining headers with active schedules, actual entries, approval records, submitted
metadata, or nonzero totals. A changed end date alone does not remove schedules.

The backfill endpoint has been removed. Setup creates headers for explicitly saved
schedule dates. GET never writes data. No scheduler is required.
