# Password reset APIs

Both endpoints are public POST requests with Content-Type: application/json.
An expired Authorization header does not block these two endpoints.

## Request a link

POST /api/auth/forgot-password

```json
{"email":"user@example.com"}
```

Returns 200 for a matching active account:

```json
{"message":"If an account exists for this email, a reset link has been sent."}
```

If no active account is linked to the email, returns 400:

```json
{"message":"No active account was found for this email address."}
```

Throttled requests return 429 with `Retry-After: 3600` and
`{"message":"Too many password reset requests. Please try again later."}`.

Email addresses are matched case-insensitively against the employee linked to an
active user. Invalid email input returns 400 with a message. The link is
`${app.mail.base-url}/reset-password?token=<token>`. Configure the base URL to the
trusted frontend URL (HTTPS in production), MAIL_ENABLED=true, and SMTP settings.
The frontend must provide the /reset-password page; no frontend is added here.

Tokens use 32 cryptographically random bytes and expire after 30 minutes. Only
SHA-256 hashes are stored in password_reset_tokens. Reset emails are sent after
commit directly, without storing the raw link in the email notification table.
Delivery failure is logged without recipient, token, link or SMTP exception details;
the response stays generic and the user may request another link. These transient
reset emails are not retried automatically or guaranteed after a process crash.
Never enable SMTP message-body debugging or log reset-page query strings.

Forgot-password requests are limited to 3 per normalized email/hour and 20 per
remote IP/hour. Reset attempts are limited to 30 per IP/15 minutes (429 with
Retry-After on excess). Limits use bounded in-memory state per app instance and
reset on restart; use a shared gateway limiter for a multi-instance deployment.
Client IP uses the servlet remote address, not an untrusted forwarded header.

## Set a new password

POST /api/auth/reset-password

```json
{"token":"token-from-email","newPassword":"new-password"}
```

Success returns 204 without a body. Password policy matches change-password:
nonblank, at least 8 Unicode characters, no more than 72 UTF-8 bytes, different
from the current password. Validation errors return 400 with a message.
Invalid, expired, used or inactive-account reset links return 400:

```json
{"message":"This reset link is invalid or expired. Please request a new one."}
```

Password update, consumption of all the user's reset tokens, refresh-token
revocation and session-version increment commit atomically. A user-row lock
serializes concurrent resets, login and refresh. Access tokens with an old session
version return 401 on subsequent requests. Pre-deployment tokens without a version
are treated as version zero and are also revoked by a reset. The normal
password-changed notification is queued in the same transaction.

The schema is created/updated by the current Hibernate ddl-auto=update setting.
Deployments with schema updates disabled must provision `password_reset_tokens` before using these endpoints.
