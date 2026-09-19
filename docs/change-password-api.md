# Change password

PUT /api/auth/change-password
Authorization: Bearer <accessToken>
Content-Type: application/json

```json
{"currentPassword":"current-password","newPassword":"new-password"}
```

The account is selected from the authenticated token. No user ID or confirmation
password is required. The current password must match. The new password must be
nonblank, at least 8 Unicode characters, at most 72 UTF-8 bytes (BCrypt limit), and
different from the current password. Passwords are not trimmed and are stored
using the configured BCrypt encoder.

Success: 204 No Content. Validation failures: 400 with a message field, for example:

```json
{"message":"Current password is incorrect."}
```

Missing, invalid, expired or inactive sessions return 401. Existing access and
refresh tokens retain their existing expiry; this endpoint does not revoke them.

After a successful change, a PASSWORD_CHANGED notification is queued in the same
transaction as the password update. The existing dispatcher sends it after commit
and retries delivery failures. The message contains no passwords or password hashes.
The recipient is the linked employee's registered email; accounts without a linked
employee or email skip notification. Delivery requires MAIL_ENABLED=true (the default
is false) and working SMTP settings. HTTP 204 confirms the password change and queue
operation, not completed email delivery.
