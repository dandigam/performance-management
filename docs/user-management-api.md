# User Management API

## List users

`GET /api/v1/user-management/users`

Returns login accounts ordered by user ID. Accounts without a linked employee are included with
`null` employee and department fields. For an unlinked account, `email` falls back to `username`.

When authentication is enabled, this endpoint requires the `ADMIN` role.

### Response

```json
[
  {
    "userId": 12,
    "username": "charan@gmail.com",
    "email": "charan@gmail.com",
    "employeeId": 2,
    "employeeCode": "RIT03",
    "employeeName": "Charan Kovvuru",
    "roleId": 31,
    "roleCode": "MANAGER",
    "roleName": "Manager",
    "departmentName": "Engineering",
    "status": "ACTIVE",
    "lastLoginAt": "2026-09-20T09:30:00",
    "createdAt": "2026-01-10T12:00:00"
  },
  {
    "userId": 13,
    "username": "admin",
    "email": "admin",
    "employeeId": null,
    "employeeCode": null,
    "employeeName": null,
    "roleId": 1,
    "roleCode": "ADMIN",
    "roleName": "Administrator",
    "departmentName": null,
    "status": "ACTIVE",
    "lastLoginAt": null,
    "createdAt": "2026-01-10T12:00:00"
  }
]
```

`lastLoginAt` is updated after each successful password login. Existing users have `null` until
their next successful login.

## Update user status

`PATCH /api/v1/user-management/users/{userId}/status`

### Request

```json
{
  "status": "INACTIVE"
}
```

The accepted values are `ACTIVE` and `INACTIVE`. Allowed transitions are:

- `ACTIVE` to `INACTIVE`
- `INACTIVE` to `ACTIVE`
- `LOCKED` to `ACTIVE`

Changing status invalidates the user's existing access and refresh tokens. Setting a locked user
to `ACTIVE` unlocks and reactivates the account.

Login attempts for `INACTIVE` or `LOCKED` accounts return `403 Forbidden` with:

```json
{
  "error": "This account is unavailable. Please contact your administrator."
}
```

Unknown users and incorrect passwords return `401 Unauthorized` with:

```json
{
  "error": "The email or password is incorrect."
}
```

### Response

Returns the updated user object using the same fields as the list endpoint. Employee and department
fields are `null` when the user has no linked employee.

## Update user role

`PUT /api/v1/user-management/users/{userId}/role`

### Request

```json
{
  "roleId": 31
}
```

The role must be an active lookup under the `SYSTEM_ROLE` lookup type. The operation updates
`users.role_id`; for a linked employee, it also closes the current `employee_roles` record and
creates the new current role record. A role change invalidates the user's existing access and
refresh tokens.

### Response

Returns the updated user object using the same fields as the list endpoint.
