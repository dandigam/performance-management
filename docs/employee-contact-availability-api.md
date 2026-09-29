# Employee contact availability

`GET /api/employees/check-contact` uses the existing employee API authentication rules.
Supply `email`, `phoneNumber`, or both as URL-encoded query parameters. For updates,
add `excludeEmployeeId` to exclude the current employee. At least one nonblank contact
is required; the optional employee ID must be positive.

Examples:

```text
GET /api/employees/check-contact?email=venkatd099%40gmail.com
GET /api/employees/check-contact?phoneNumber=1234567890
GET /api/employees/check-contact?email=venkatd099%40gmail.com&phoneNumber=1234567890&excludeEmployeeId=42
```

An existing employee email returns HTTP 400:

```json
{"type":"WARNING","code":"INVALID_OPERATION","message":"Employee email already exists: venkatd099@gmail.com"}
```

An existing employee phone returns HTTP 400:

```json
{"type":"WARNING","code":"INVALID_OPERATION","message":"Employee phone number already exists: 1234567890"}
```

Available contacts return HTTP 200:

```json
{"type":"SUCCESS","message":"Employee contact details are available"}
```

Email matching is case-insensitive; leading/trailing whitespace is removed from both
fields. Phone formatting is otherwise unchanged. When both fields conflict, email is
reported first. Encode a phone's leading `+` as `%2B`.

This read-only check searches employee contacts, not standalone account usernames.
Availability is advisory: create/update validation and database uniqueness remain in
place, and employee creation also checks account username conflicts.
