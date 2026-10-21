# Employee Workforce Report API

## Report definition

`GET /api/v1/reports/employees/definition`

Returns the backend allowlist used to build the report UI. Each entry describes its display type,
lookup source or fixed options, visibility, sorting support, filtering support, and accepted
operators.

## Metadata-driven query

`POST /api/v1/reports/employees/query`

```json
{
  "columns": ["employeeNumber", "employeeName", "departmentName", "assignmentStatus"],
  "filters": [
    { "field": "assignmentStatus", "operator": "EQUALS", "value": "UNASSIGNED" },
    { "field": "status", "operator": "EQUALS", "value": "ACTIVE" }
  ],
  "sort": [
    { "field": "employeeName", "direction": "ASC" }
  ],
  "page": 0,
  "size": 25
}
```

Only fields and operators published by the definition endpoint are accepted. `IN` requires an
array value. Lookup ID fields such as `departmentId` and `designationId` require numeric IDs;
lookup code fields such as `status` and `workMode` require codes.

The response contains only the requested columns. Its summary counts apply every current filter,
including `assignmentStatus`.

```json
{
  "summary": {
    "totalEmployees": 7,
    "assignedEmployees": 1,
    "unassignedEmployees": 6
  },
  "columns": [
    { "key": "employeeName", "label": "Employee", "type": "TEXT" },
    { "key": "departmentName", "label": "Department", "type": "TEXT" },
    { "key": "assignmentStatus", "label": "Assignment status", "type": "ENUM" }
  ],
  "content": [
    {
      "employeeName": "Charan Kovvuru",
      "departmentName": null,
      "assignmentStatus": "UNASSIGNED"
    }
  ],
  "page": 0,
  "size": 25,
  "totalElements": 6,
  "totalPages": 1
}
```

The original query-parameter endpoint remains available for compatibility.

## Excel export

`POST /api/v1/reports/employees/export`

The request uses the same `columns`, `filters`, and `sort` fields as the metadata-driven query.
Pagination is omitted because the export contains every matching employee.

```json
{
  "columns": [
    "employeeNumber",
    "employeeName",
    "departmentName",
    "workMode",
    "joiningDate"
  ],
  "filters": [
    { "field": "workMode", "operator": "EQUALS", "value": "ONSITE" }
  ],
  "sort": [
    { "field": "employeeName", "direction": "ASC" }
  ]
}
```

The response is an Excel workbook with a filename such as
`employee-workforce-2026-09-26.xlsx`. It contains the requested columns in the requested order,
uses metadata labels for headers, displays lookup labels, writes dates as Excel date cells, and
leaves null values blank. When `columns` is empty or omitted, the metadata default columns are
used.

## List employee workforce records

`GET /api/v1/reports/employees`

### Query parameters

| Parameter | Description |
| --- | --- |
| `search` | Searches employee number, employee name, email, department name, and designation name. |
| `departmentId` | Active `DEPARTMENT` lookup value ID. |
| `designationId` | Active `DESIGNATION` lookup value ID. |
| `employmentType` | Active `EMPLOYMENT_TYPE` lookup code. |
| `workMode` | Active `WORK_MODE` lookup code. |
| `workLocation` | Active `WORK_LOCATION` lookup code. |
| `status` | Active `EMPLOYEE_STATUS` lookup code. |
| `page` | Zero-based page number. Default: `0`. |
| `size` | Page size from 1 through 100. Default: `25`. |
| `sort` | `field,asc` or `field,desc`. Default: `employeeName,asc`. |

Supported sort fields are `employeeId`, `employeeNumber`, `employeeName`, `email`,
`employmentType`, `workMode`, `workLocation`, `status`, and `joiningDate`.

An employee's department is returned when all current project assignments belong to the same
department. It is `null` when the employee has no current department or has current assignments
in multiple departments.

### Response

```json
{
  "summary": {
    "totalEmployees": 1
  },
  "content": [
    {
      "employeeId": 1,
      "employeeNumber": "RIT01",
      "employeeName": "Employee Name",
      "email": "employee@example.com",
      "departmentName": "Engineering",
      "designationName": "Technical Lead",
      "employmentType": "FULL_TIME",
      "workMode": "OFFSHORE",
      "workLocation": "REMOTE",
      "status": "ACTIVE",
      "joiningDate": "2026-01-01"
    }
  ],
  "page": 0,
  "size": 25,
  "totalElements": 1,
  "totalPages": 1
}
```
