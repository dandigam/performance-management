# Employee organization hierarchy

Base URL: `/api/v1/employee-org-hierarchy`.

This is a directory of current reporting relationships, with one record per employee.
It does not change performance reviewers, SOW assignments, or other approval workflows.
Reporting history and effective dates are outside this first CRUD implementation.
Existing global API authentication rules apply. Export always requires authentication,
including when the local development authentication bypass is enabled. No new role rules are added.

| Method | Path | Success |
| --- | --- | --- |
| POST | base URL | 201 with record and Location header |
| POST | `/bulk` | 201 with list of created records |
| GET | base URL | 200 with list ordered by record ID |
| GET | `/export` | 200 with XLSX attachment; authentication required |
| GET | `/{id}` | 200 with record |
| GET | `/employee/{employeeId}` | 200 with employee's reporting record |
| PUT | `/{id}` | 200 with updated record |
| DELETE | `/{id}` | 204, no body |

Export uses worksheet `Organization Hierarchy` with Employee ID (text RIT ID), Employee name,
Reporting manager 1, Reporting manager 2, and Status. It includes all employees, including
inactive employees and employees without hierarchy records, ordered by name then RIT ID.
Managers are resolved only from organization reporting records; incomplete/cyclic chains
produce blank unavailable manager names. There is no UI pagination or tree-state filtering.
Values are text cells, never formulas. Headers are bold, frozen, and filterable. An empty
employee table produces headers only. Failures return the existing 500 API error response.
The response Content-Type is `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`
and Content-Disposition is `attachment; filename="employee-organization-hierarchy.xlsx"`.

POST and PUT body:

```json
{
  "employeeId": 101,
  "reportingManagerId": 102
}
```

Bulk create accepts one manager for multiple employees:

```json
{
  "employeeList": [{"employeeId": 101}, {"employeeId": 103}],
  "reportingManagerId": 102
}
```

The list must be nonempty and every entry must have a positive employeeId. Repeated
employee IDs return 400. Existing reporting records return 409; bulk create does not
overwrite them. Every row is saved in one transaction: any failure rolls back the entire
batch. A successful response is an array using the same record format as single create.

Use `reportingManagerId: null` for a top-level employee. IDs must refer to existing
employees. PUT requires the original employeeId; it changes that employee's manager,
not the identity of the employee represented by the record. Names and auditing fields
are returned in the response; actor IDs come from the authenticated user.

Missing records/employees return 404, duplicate employee entries return 409, and invalid
IDs or reporting cycles return 400. Circular chains and self-reporting are rejected.
Deleting a record removes only that employee's manager relationship. It does not delete
the employee or change people who report to that employee. Such a deletion can truncate
the reporting chain; consumers should treat an absent relationship as unconfigured.

Apply `database/migrations/20261022_create_employee_org_hierarchy.sql` against the target
database, or let the existing Hibernate `ddl-auto=update` setting create the new table
on startup. The SQL is a manual script, not an automatically executed migration.
