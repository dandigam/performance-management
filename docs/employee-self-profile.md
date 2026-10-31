# Authenticated employee self-profile

```http
GET /api/employees/me
Authorization: Bearer <access-token>
```

No request body or employee ID is accepted for selecting the profile. The authenticated
account's linked employee determines the response, including for HR/Admin callers.
Requires an active account with a linked employee. Success returns 200 with a direct
EmployeeSelfProfileResponse JSON object and Cache-Control: no-store.

The response includes identity/contact information, employment details, role and
designation, current projects, address, professional details, education and experience.
Compensation, bank details, documents and performance review data are not fields in
this DTO. Existing profile mapping is reused internally; the API serializes only the
dedicated self-profile allowlist.

Frontend self-profile screens should use this route instead of supplying a stored
employee ID to GET /api/employees/{employeeId}. Adding this endpoint does not change
or secure the existing ID-based routes; their access-control work remains separate.
No database migration or request changes to existing endpoints are required.

## Own documents

`GET /api/employees/me/documents` uses the same Bearer token and resolves the employee
from the authenticated account. It returns 200 with a direct array of DocumentResponse
objects, or an empty array when no documents exist, with Cache-Control: no-store.
No employee ID is accepted for selecting the document list. Existing document mapping
is reused. This adds a self-document listing only; existing ID-based document and
download routes have not had their authorization changed by this addition.
