# Employee directory summaries

`GET /api/employees/summaries?page=0&size=20`

The existing `/api/employees` endpoint is unchanged. Summary filters apply in the database before counting and pagination. The response retains `content`, `page`, `size`, `totalElements`, `totalPages`, `first`, and `last`.

| Parameter | Behavior |
| --- | --- |
| `search` | Case-insensitive substring of full name, RIT ID, email, designation, current SOW name, or current SOW department name. `%` and `_` are literal search characters. |
| `departmentId` | At least one current assignment to a SOW in this department. |
| `sowId` | At least one current assignment to this SOW. |
| `assignmentStatus` | `ASSIGNED` or `UNASSIGNED`, based on whether a current assignment exists. |
| `workMode` | `ONSITE` or `OFFSHORE`. |
| `status` | Employee status: `ACTIVE` or `INACTIVE`. |
| `sort` | `field,asc` or `field,desc`; default `employeeName,asc`. Supported fields: `employeeName`, `employeeId`, `ritId`, `email`, `employmentType`, `workMode`, `workLocation`, `status`. |
| `page` | Zero-based, default 0. |
| `size` | 1–100, default 20. |

Absent or blank text filters are ignored. Supplied filters combine with AND. When both department and SOW filters are present, the same current SOW assignment must match both. Status and work mode values accept either case. Invalid filter values, IDs, pagination, or sort fields return HTTP 400.

A current assignment has status `ASSIGNED`, starts on or before today, and has no end date or ends on or after today. Completed, future, and expired assignments do not qualify. Multiple assignments do not duplicate employees or inflate counts. Current projects are deduplicated by SOW. Department fields are null when no department is available or current projects span multiple departments.

Examples:

```http
GET /api/employees/summaries?page=0&size=20&status=ACTIVE
GET /api/employees/summaries?departmentId=5&status=ACTIVE&sort=employeeName,asc
GET /api/employees/summaries?search=charan&sowId=20&workMode=ONSITE
GET /api/employees/summaries?assignmentStatus=UNASSIGNED
```

If 53 employees match, page 0 with size 20 returns 20 items, `totalElements: 53`, and `totalPages: 3`.
