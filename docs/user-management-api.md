# User Management API

## Create user

`POST /api/v1/user-management/users`

Creates a login account with an optional employee link. The username and email must be unique.
For an account without an employee, `email` must match `username` because the current user model
uses the username as the account email.

```json
{
  "username": "finance@rit.com",
  "email": "finance@rit.com",
  "roleId": 3,
  "employeeId": null,
  "sendInvitation": true
}
```

The role must be an active `SYSTEM_ROLE`. When `employeeId` is supplied, that employee must not
already have a user account and the request email must match the employee email.

New accounts have `INVITED` status. When `sendInvitation` is `true`, the user receives a one-time
password creation link that expires after 24 hours. Successfully setting the password changes the
account status to `ACTIVE`.

Returns `201 Created` with the created user object.

## List users

`GET /api/v1/user-management/users`

Returns login accounts ordered by user ID. Accounts without a linked employee are included with
`null` employee fields. For an unlinked account, `email` falls back to `username`.

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
  "type": "WARNING",
  "code": "ACCOUNT_UNAVAILABLE",
  "message": "This account is unavailable. Please contact your administrator."
}
```

Unknown users and incorrect passwords return `401 Unauthorized` with:

```json
{
  "type": "WARNING",
  "code": "AUTHENTICATION_FAILED",
  "message": "The email or password is incorrect."
}
```

### Response

Returns the updated user object using the same fields as the list endpoint. Employee fields are `null` when the user has no linked employee.

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

## Portal access metadata

`POST /api/v1/user-management/users` accepts optional `portalAccess`: `FULL`,
`ONBOARDING`, or `BLOCKED`. Omitted or null defaults to `FULL`. Send
`"portalAccess": "ONBOARDING"` when creating an onboarding account.

`PUT /api/v1/user-management/users/{userId}/portal-access` updates this field:

```json
{ "portalAccess": "FULL" }
```

Returns `204 No Content`. Omitted or null leaves the existing value unchanged.
Invalid values, including blank strings, fail validation. Existing ADMIN
permissions apply to this endpoint.

Login, refresh, and `/api/auth/me` responses include `portalAccess`.
User management list, create, status-update, and role-update responses also include `portalAccess`.
This field is metadata only; it does not enforce access restrictions or change
account status or token validity. Apply
`database/migrations/2026-09-27-user-portal-access.sql` before deploying.
The migration initializes existing users to `FULL`.
