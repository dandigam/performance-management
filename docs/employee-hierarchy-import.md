# Employee organization hierarchy Excel import

The separate `EmployeeHierarchyImportController` serves
`/api/v1/employee-org-hierarchy/import`. All endpoints require ADMIN or HR.

- `GET /template`: download the supported text-formatted XLSX template.
- `POST /validate`: multipart upload with field `file`; returns an import ID and row preview.
- `POST /{importId}/commit`: revalidate and atomically apply the whole batch. Repeating a successful commit returns its saved result.
- `GET /{importId}`: retrieve the persisted preview or committed result.
- `GET /{importId}/errors`: download row errors as XLSX.

Columns, in order: RACF ID, First name, Last name, Email, Reporting manager RACF ID, Employee location, Gender, System Role, Designation.
RACF IDs are trimmed and matched without case sensitivity. Duplicate RACF rows are errors.
First name, last name and email are required only when creating a new employee.
Existing names, email addresses and RIT IDs are preserved. Nonblank Employee location, Gender, System Role and Designation cells insert or update those fields; blank cells preserve stored values.
A supplied email for an existing employee is ignored along with the supplied names.
Blank manager cells preserve existing relationships. There is no manager-clearing operation in this import.
Managers must exist already or appear as an employee row in the same file.
Use the ordinary hierarchy API to clear a reporting manager explicitly.

New employees are ACTIVE with an automatically generated RIT ID using the existing RIT format.
Missing linked portal accounts and current employee-role mappings are created at commit. Onboarding, assignments, invitation tokens and invitation emails are not created. Active employees are eligible
for ALL and explicit EMPLOYEE performance cycle scopes without onboarding; department or designation
scopes still require their normal matching data, and configured reporting stages require valid managers.

The preview describes INSERT / UPDATE / UNCHANGED / ERROR hierarchy outcomes and the count of
new employees. All rows must be valid to commit. This keeps connected reporting groups consistent
and allows final-graph validation independently of Excel row order. Commit rechecks current data
and takes the same hierarchy write lock used by the existing hierarchy service.

Limit: one data worksheet (plus the template's very-hidden Options worksheet), 2000 data rows, 5 MB. Cells must be text; formulas and numeric cells are rejected.
Validation stores the uploader, upload time, filename, original workbook, counts and row outcomes.
Successful commit saves the result in the same transaction as employee, account, role and hierarchy changes.
A failed database transaction rolls back the batch; callers can retry after fixing the underlying conflict.

The `EmployeeHierarchyImport` JPA entity maps the persistent import audit table
`employee_hierarchy_imports`. The current development and production configurations use
`spring.jpa.hibernate.ddl-auto=update`, so Hibernate creates this table from the entity at startup.
No separate import migration script is required with these configurations.
Existing schemas can retain old NOT NULL constraints even with Hibernate schema update enabled.
Performance-only employees require `employees.joining_date` to allow NULL, as declared by the entity.
In the prod profile, `ProductionReferenceDataInitializer` checks the existing column at startup
and applies the following adjustment only when it is still required. For other existing MySQL
environments, the same one-time adjustment can be applied manually:

```sql
ALTER TABLE employees MODIFY COLUMN joining_date DATE NULL;
```

This preserves existing joining dates and permits new performance-only employees to omit them.
RACF duplicates are checked by the import service, but the current employee entity does not
declare a normalized RACF unique database constraint. Concurrent employee creation outside
the hierarchy import lock is therefore not protected by a database RACF uniqueness constraint.
Existing employees with missing RACF IDs remain unchanged and cannot be addressed by this import.

## UI API contract

The employee list endpoint `GET /api/employees/summaries` includes `racfId` in each
`content` entry, mapped from the employee's stored `csxRacfId`. It is null when no RACF ID
is stored. Use this field to match list entries to imported Excel rows without case sensitivity.
The summaries `search` parameter also matches RACF IDs without case sensitivity and supports
partial matches, for example `/api/employees/summaries?page=0&size=20&search=Z9083&sort=employeeName,asc`.
For example, an employee entry includes these identifier fields (other summary fields omitted):

```json
{
  "employeeId": 8,
  "employeeName": "Siva Krishnam Raju Kanumuri",
  "ritId": "RIT08",
  "racfId": "I5892",
  "email": "kskraju54@gmail.com"
}
```

Base path: `/api/v1/employee-org-hierarchy/import`.
Every request requires `Authorization: Bearer <access-token>` for an ADMIN or HR user.
Examples below describe the implemented contract; they are not captured live responses.
JSON success responses are returned directly, without a `data` wrapper.

### 1. Download template

Request (no body or query parameters):

```http
GET /api/v1/employee-org-hierarchy/import/template
Authorization: Bearer <access-token>
Accept: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
```

Response:

```http
HTTP/1.1 200 OK
Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
Content-Disposition: attachment; filename="employee-hierarchy-template.xlsx"

<binary XLSX file>
```

The workbook contains a data worksheet named `Hierarchy` and a very-hidden `Options` worksheet supporting the dropdowns, with these columns in this order:

| Column | Required | Behavior |
| --- | --- | --- |
| RACF ID | Always | Trimmed and converted to uppercase; identifies the employee |
| First name | New employees only | Trimmed; ignored for existing employees |
| Last name | New employees only | Trimmed; ignored for existing employees |
| Email | New employees only | Trimmed and lowercased; ignored for existing employees |
| Reporting manager RACF ID | No | Trimmed and converted to uppercase; resolves the manager employee ID |
| Employee location | No | Dropdown of active WORK_MODE codes ONSITE and OFFSHORE; stores the code in employees.work_mode |
| Gender | No | MALE, FEMALE, NON_BINARY, OTHER, PREFER_NOT_TO_SAY |
| System Role | No | Dropdown of permitted active SYSTEM_ROLE names; updates account and employee role mapping |
| Designation | No | Dropdown of active DESIGNATION names; stores the lookup ID |

Example Excel data (all cells must be text):

| RACF ID | First name | Last name | Email | Reporting manager RACF ID |
| --- | --- | --- | --- | --- |
| EMP001 | Alex | Morgan | alex.morgan@example.com | MGR001 |

`MGR001` must already exist or appear as an employee row in the same upload.
Blank manager cells preserve the current manager; for a new hierarchy record the manager remains null.

### 2. Upload and validate

Request: `multipart/form-data` containing exactly the file part named `file`.
No JSON body, uploader field, or import ID is required. The uploader comes from authentication.

```http
POST /api/v1/employee-org-hierarchy/import/validate
Authorization: Bearer <access-token>
Accept: application/json
Content-Type: multipart/form-data; boundary=<generated-boundary>

--<generated-boundary>
Content-Disposition: form-data; name="file"; filename="employees.xlsx"
Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet

<binary XLSX file>
--<generated-boundary>--
```

Browser request:

```javascript
const form = new FormData();
form.append("file", selectedFile);
const response = await fetch("/api/v1/employee-org-hierarchy/import/validate", {
  method: "POST",
  headers: { Authorization: `Bearer ${accessToken}`, Accept: "application/json" },
  body: form,
});
const result = await response.json();
```

Let the browser set multipart `Content-Type` and its boundary.

Success response: `200 OK`, `Content-Type: application/json`.
This example assumes EMP001 is new and MGR001 exists:

```json
{
  "importId": "73c71615-1481-49a5-b225-a51ff0c76454",
  "status": "VALIDATED",
  "message": "Existing employee details are preserved. Blank manager cells keep the current manager. New employees are ACTIVE with generated RIT IDs. Missing portal accounts and employee roles will be created. No invitations will be sent. All rows must be valid before this batch can be committed.",
  "newEmployees": 1,
  "insertedHierarchy": 1,
  "updatedHierarchy": 0,
  "unchanged": 0,
  "errors": 0,
  "rows": [
    {
      "rowNumber": 2,
      "racfId": "EMP001",
      "firstName": "Alex",
      "lastName": "Morgan",
      "email": "alex.morgan@example.com",
      "managerRacfId": "MGR001",
      "action": "INSERT",
      "errors": []
    }
  ]
}
```

Row validation errors also return **200 OK**, with `status: "INVALID"`.
For example, if MISSING01 is not in employees or the file:

```json
{
  "importId": "dbfb56fc-ac17-4b08-9418-246d230d0357",
  "status": "INVALID",
  "message": "Existing employee details are preserved. Blank manager cells keep the current manager. New employees are ACTIVE with generated RIT IDs. Missing portal accounts and employee roles will be created. No invitations will be sent. All rows must be valid before this batch can be committed.",
  "newEmployees": 0,
  "insertedHierarchy": 0,
  "updatedHierarchy": 0,
  "unchanged": 0,
  "errors": 1,
  "rows": [
    {
      "rowNumber": 2,
      "racfId": "EMP001",
      "firstName": "Alex",
      "lastName": "Morgan",
      "email": "alex.morgan@example.com",
      "managerRacfId": "MISSING01",
      "action": "ERROR",
      "errors": ["Manager RACF ID not found in employees or file"]
    }
  ]
}
```

Validation stores the upload and preview only; it does not create employees, accounts, role mappings, or hierarchy relationships.
Invalid file structure is an HTTP 400 error, rather than an INVALID row preview (see errors below).

### 3. Commit import

Request (no body or query parameters):

```http
POST /api/v1/employee-org-hierarchy/import/73c71615-1481-49a5-b225-a51ff0c76454/commit
Authorization: Bearer <access-token>
Accept: application/json
```

Successful response: `200 OK`, `Content-Type: application/json`.
Assuming the database has not changed since the valid preview:

```json
{
  "importId": "73c71615-1481-49a5-b225-a51ff0c76454",
  "status": "COMMITTED",
  "message": "Hierarchy imported. Missing portal accounts and employee roles were created. No invitations were sent.",
  "newEmployees": 1,
  "insertedHierarchy": 1,
  "updatedHierarchy": 0,
  "unchanged": 0,
  "errors": 0,
  "rows": [
    {
      "rowNumber": 2,
      "racfId": "EMP001",
      "firstName": "Alex",
      "lastName": "Morgan",
      "email": "alex.morgan@example.com",
      "managerRacfId": "MGR001",
      "action": "INSERT",
      "errors": []
    }
  ]
}
```

Commit revalidates the saved file against current data, so counts/actions can change from preview.
If revalidation fails, the response is **200 OK** with the same INVALID response structure shown
for validation, containing the newly computed counts and row errors. No employee, account, role or hierarchy
changes are applied. Check `status`, not HTTP success alone, before reporting an import as completed.

Repeating a successful commit for the same import ID returns its saved COMMITTED response.
There is no partial success: the complete batch commits or rolls back. Correcting the Excel file
requires another `/validate` upload, which returns a new import ID.

### 4. Retrieve status and saved results

Request (no body or query parameters):

```http
GET /api/v1/employee-org-hierarchy/import/73c71615-1481-49a5-b225-a51ff0c76454
Authorization: Bearer <access-token>
Accept: application/json
```

Response after successful commit: `200 OK`, `Content-Type: application/json`.

```json
{
  "importId": "73c71615-1481-49a5-b225-a51ff0c76454",
  "status": "COMMITTED",
  "message": "Hierarchy imported. Missing portal accounts and employee roles were created. No invitations were sent.",
  "newEmployees": 1,
  "insertedHierarchy": 1,
  "updatedHierarchy": 0,
  "unchanged": 0,
  "errors": 0,
  "rows": [
    {
      "rowNumber": 2,
      "racfId": "EMP001",
      "firstName": "Alex",
      "lastName": "Morgan",
      "email": "alex.morgan@example.com",
      "managerRacfId": "MGR001",
      "action": "INSERT",
      "errors": []
    }
  ]
}
```

Before commit, this returns the saved VALIDATED or INVALID preview with the identical schema.
It does not revalidate, refresh the database state, or start an import. Commit is synchronous;
there are no PROCESSING or FAILED status values in the current implementation.

### 5. Download error report

Request (no body or query parameters):

```http
GET /api/v1/employee-org-hierarchy/import/dbfb56fc-ac17-4b08-9418-246d230d0357/errors
Authorization: Bearer <access-token>
Accept: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
```

Response:

```http
HTTP/1.1 200 OK
Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
Content-Disposition: attachment; filename="employee-hierarchy-errors.xlsx"

<binary XLSX file>
```

The report is generated from the saved row outcomes:

| Excel row | RACF ID | Errors |
| --- | --- | --- |
| 2 | EMP001 | Manager RACF ID not found in employees or file |

Multiple errors on a row are separated by `; `. If there are no errors, the workbook contains
only column headers. Both download endpoints return a Blob/ArrayBuffer on success; failures
return JSON instead. Use the documented filename as a client fallback if the browser cannot
read the Content-Disposition header across origins.

### Shared JSON response fields

| Field | Type | Meaning |
| --- | --- | --- |
| importId | string | UUID identifying the saved upload |
| status | string | VALIDATED, INVALID, or COMMITTED |
| message | string | Informational text for the user |
| newEmployees | integer | Valid rows that would create employees; actual created count after COMMITTED |
| insertedHierarchy | integer | Valid INSERT hierarchy rows; applied count after COMMITTED |
| updatedHierarchy | integer | Valid UPDATE hierarchy rows; applied count after COMMITTED |
| unchanged | integer | Valid rows whose existing hierarchy needs no change |
| errors | integer | Number of invalid rows, not number of individual error messages |
| rows | array | Nonblank Excel data rows, in file order |
| rows[].rowNumber | integer | Original Excel row number; header is row 1 |
| rows[].racfId | string | Normalized employee RACF ID |
| rows[].firstName | string | Trimmed uploaded first name; not the resolved database value |
| rows[].lastName | string | Trimmed uploaded last name; not the resolved database value |
| rows[].email | string | Normalized uploaded email; not the resolved database value |
| rows[].managerRacfId | string | Normalized uploaded manager RACF ID, or empty string |
| rows[].action | string | INSERT, UPDATE, UNCHANGED, or ERROR for the hierarchy record |
| rows[].errors | string array | Empty for valid rows; one or more messages for invalid rows |

`newEmployees` overlaps `insertedHierarchy`; do not add these together as a row total.
The number of rows is `insertedHierarchy + updatedHierarchy + unchanged + errors`.
Only `status: "COMMITTED"` means changes have been applied. An existing employee without a
hierarchy record has action INSERT but does not increase newEmployees. New internal employee
IDs, generated RIT IDs, and resolved manager names are not returned by these endpoints.

### HTTP errors for all endpoints

Error responses use `Content-Type: application/json` and this separate schema:

```json
{
  "type": "WARNING",
  "code": "INVALID_OPERATION",
  "message": "Upload a nonempty .xlsx file of at most 5 MB"
}
```

| HTTP status | code | When |
| --- | --- | --- |
| 400 | INVALID_OPERATION | Empty/wrong file type, service 5 MB limit, bad headers, multiple sheets, over 2000 rows, non-text cells, unreadable workbook, existing duplicate RACF IDs, or generated RIT ID collision |
| 400 | MISSING_MULTIPART_FIELD | Missing multipart file part |
| 400 | INVALID_REQUEST | Malformed request handled by the shared request-value handler |
| 400 | REQUIRED_DATABASE_VALUE | Database still requires a value omitted by this operation; check schema compatibility |
| 401 | AUTHENTICATION_REQUIRED | Authentication required by the security filter chain |
| 403 | ACCESS_DENIED | User lacks ADMIN/HR authorization |
| 404 | RESOURCE_NOT_FOUND | Unknown import ID on status, commit, or error download |
| 409 | DUPLICATE_RESOURCE | Database integrity conflict during commit; transaction rolls back |
| 409 | DATABASE_CONSTRAINT_VIOLATION | Other database constraint violation; transaction rolls back |
| 413 | UPLOAD_TOO_LARGE | Server multipart size limit exceeded before service validation |
| 500 | INTERNAL_ERROR | Unexpected server failure; type is ERROR |

Example unknown-import response (`404 Not Found`):

```json
{
  "type": "WARNING",
  "code": "RESOURCE_NOT_FOUND",
  "message": "Import not found: 73c71615-1481-49a5-b225-a51ff0c76454"
}
```

Example unauthenticated response (`401 Unauthorized`):

```json
{
  "type": "WARNING",
  "code": "AUTHENTICATION_REQUIRED",
  "message": "Authentication is required."
}
```

Example database-conflict response (`409 Conflict`):

```json
{
  "type": "WARNING",
  "code": "DUPLICATE_RESOURCE",
  "message": "A record with the same unique value already exists."
}
```

Example unexpected-failure response (`500 Internal Server Error`):

```json
{
  "type": "ERROR",
  "code": "INTERNAL_ERROR",
  "message": "Unable to complete the request. Please try again."
}
```

Error messages may vary; branch on HTTP status and code. The shared 413 handler currently
mentions SOW PDF attachments in its message; the hierarchy import service limit remains 5 MB.
No import ID or downloadable row report is created when `/validate` fails with an HTTP error.


## Portal accounts created by import

Commit also ensures each employee row has a linked `users` account and a current
`employee_roles` mapping. This applies even when the hierarchy action is UNCHANGED.
Managers referenced by RACF ID are not provisioned unless they also have an employee row in the file.

- New employees receive the supplied System Role, or the active SYSTEM_ROLE / EMPLOYEE lookup role when blank.
- Existing employees with an account keep its username, password, status and portal access. Its role changes only when System Role is supplied.
- If an account exists but its current employee-role mapping is missing, the mapping uses that account's role.
- If an account is missing, the employee's current role mapping is reused when present; otherwise EMPLOYEE is used.
- Missing accounts use the stored employee email, never the ignored uploaded email for an existing employee.
- New accounts for ACTIVE employees use status INVITED (awaiting setup), a random encoded password,
  and the application's default FULL portal access. INACTIVE employees receive INACTIVE accounts.
- No invitation token, invitation email, admin notification email or onboarding record is generated by import.
- Account creation and role mapping are rolled back with the entire batch if any write fails.

Preview validates account email format, the 50-character username limit, username availability,
and the role needed for each missing account. These checks also run again at commit.
The JSON schema and counters are unchanged: actions/counters describe hierarchy changes,
and newEmployees counts employee creations; no separate account count is returned.
Stored results from imports completed before account provisioning was implemented are preserved.
Reupload those employees and commit a new import to provision their missing accounts.

An ADMIN can later find the user ID using `GET /api/v1/user-management/users` and explicitly send setup:

```http
POST /api/v1/user-management/users/{userId}/send-login-setup
Authorization: Bearer <admin-access-token>
```

No request body is required. Success returns `204 No Content`. This action sends the invitation;
import itself never invokes it. Existing user-management permissions and account status checks apply.

## Additional employee fields and dropdowns

Download a fresh template to obtain the four new columns and current active options.
The original five-column format remains accepted, including previously validated uploads.
Lookup names or codes are accepted without case sensitivity. Unknown, inactive or ambiguous
values produce row errors; edited dropdown contents never override server reference data.
Gender also accepts spaces or hyphens in place of underscores.
Employee location accepts only active WORK_MODE values ONSITE and OFFSHORE and updates work_mode; work_location is preserved.
All four columns are optional: blank cells preserve existing values and leave new employee
fields unset; a blank new employee role defaults to EMPLOYEE. These columns cannot clear values.

Only ADMIN can assign privileged roles or change an existing privileged account's role.
HR templates offer only EMPLOYEE, and HR cannot use pasted values to bypass this restriction.
Permissions and lookup availability are rechecked under the committing caller's authentication.
Role updates synchronize users.role and the current employee_roles mapping, close the previous
mapping using the business date, and invalidate existing access sessions via sessionVersion.
No invitations or OTPs are generated by this import.

Preview rows additionally expose employeeLocation, gender, systemRole and designation with
uploaded values (gender normalized). Actions and counters continue to describe hierarchy
changes only: an UNCHANGED hierarchy row can still update these four employee fields.
The entire upload, including employee details and role changes, commits atomically.
Existing stored previews may omit the new properties until commit revalidates the saved file.
The earlier five-column request/response examples remain valid examples for legacy uploads;
new previews include the four additional row properties. Example values should come from
the dropdowns in the downloaded template rather than from hardcoded location/designation lists.