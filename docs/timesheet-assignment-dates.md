# Timesheet setup dates and assignment identity

Setup now separates the planned scheduling window from the actual resource assignment.

| API field | Database column | Meaning |
| --- | --- | --- |
| `plannedStartDate` | `planned_start_date` | Admin's first planned date |
| `plannedEndDate` | `planned_end_date` | Admin's last planned date |
| `assignmentStartDate` | `assignment_start_date` | Actual resource assignment start |
| `assignmentEndDate` | `assignment_end_date` | Actual completion date; null while ongoing |
| `milestonePositionAssignmentId` | `milestone_position_assignment_id` | Specific resource assignment period |
| `workType` | `work_type` | PROJECT or INTERNAL |
| `internalWorkType` | `internal_work_type` | Required category for INTERNAL, e.g. TRAINING |

For PROJECT setup the assignment dates come from the linked resource, not the planned dates. The resource must belong to the same employee, SOW, and milestone. Complete the resource via unassign; the setup becomes COMPLETED and receives the actual end date while its planned dates are preserved. COMPLETED setups cannot be edited or reopened. A return assignment can have a separate setup for the same employee/SOW/milestone.

POST/PUT `/api/v1/employees/{employeeId}/timesheet-projects` accepts an array:

```json
[{
  "workType": "PROJECT",
  "sowId": 20,
  "milestoneId": 129,
  "milestonePositionAssignmentId": 7,
  "plannedStartDate": "2026-01-01",
  "plannedEndDate": "2026-12-31",
  "scheduleDates": [],
  "deletedDates": [],
  "level1ApproverId": 5,
  "level2ApproverId": 6,
  "status": "ACTIVE"
}]
```

For INTERNAL, omit SOW/milestone/resource IDs and provide `internalWorkType`. Actual start defaults to the initial planned start unless supplied. Actual end remains null unless completing internal work with status COMPLETED. Internal weekly setup/schedule rows are supported; this change does not introduce an internal hours-entry submission API.

Update by `timesheetEmployeeProjectId`. Read a particular setup with GET `/api/v1/employees/{employeeId}/timesheet-projects/setups/{setupId}`. The legacy SOW/milestone detail route rejects ambiguous multiple periods rather than choosing one silently.

Old `startDate`/`endDate` request and response names remain compatibility aliases for the **planned** dates. Old project requests without a resource ID are accepted only when there is exactly one ASSIGNED role matching the employee/SOW/milestone. Clients should send the explicit ID.

Scheduling and week visibility use the intersection of planned and actual dates. Existing future schedule records are not deleted by completion in this change. No two-month limit is imposed.

## Migration

Stop the backend, apply `allow_completed_timesheet_project_status.sql` if needed, then apply `separate_timesheet_assignment_and_planned_dates.sql`, then restart. Run the date migration **before** Hibernate schema update; otherwise Hibernate may add empty planned-date columns beside the old columns.

The migration renames existing dates without deleting records. It links only setups with exactly one overlapping resource assignment, and reports unresolved rows for manual matching. For completed linked assignments, the actual end comes from the resource record. Previously overwritten planned ends cannot be reconstructed automatically. Review unresolved setups before editing or unassigning their associated resource. The old employee/SOW/milestone unique index is replaced with a unique resource-assignment link.
