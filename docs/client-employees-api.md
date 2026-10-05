# Client employees

The existing `/api/v1/csx-employees` endpoints support employees for any client,
including CSX and CORYS. The existing URL and `csx_employees` table name are retained.

`POST /api/v1/csx-employees` and `PUT /api/v1/csx-employees/{id}` require a positive
`clientId` referencing `clients.id`, alongside the existing employee fields:

```json
{
  "clientId": 2,
  "firstName": "Jane",
  "lastName": "Doe",
  "email": "jane@example.com",
  "status": "ACTIVE"
}
```

Use the actual ID returned by the clients API; no ID is hardcoded for CSX or CORYS.
Missing/non-positive client IDs return 400; unknown clients return 404. PUT replaces
the client selection, so send `clientId` on every update. Other PUT field semantics
remain unchanged. Email uniqueness remains global across client employees.

Create, update, list, and detail responses now include `clientId` and `clientName`.
Unassigned legacy employees return null for both fields.

- `GET /api/v1/csx-employees`: all employees, including unassigned legacy rows.
- `GET /api/v1/csx-employees?clientId=2`: only that client's employees.
- `GET /api/v1/csx-employees/{id}`: one employee with client details.

Lists are sorted by first name, last name, then ID. A known client with no employees
returns `[]`; an unknown client filter returns 404 and a non-positive filter returns 400.
Existing endpoint authentication rules remain in effect.

Apply `database/migrations/2026-10-05-client-employee-client.sql` once when Hibernate
schema updates are disabled. It adds nullable `client_id`, an index and a foreign key;
it does not guess client assignments for existing records or create client rows.
The API requires a client for new writes while keeping legacy rows readable.

The frontend should supply a client selector, show `clientName`, and use the client
filter for client-specific lists/contact selectors. Frontend source and SOW contact
validation are outside this change; SOW writes do not yet enforce matching client IDs.
