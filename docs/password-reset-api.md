# Password reset APIs

All three endpoints are public POST requests with Content-Type: application/json.
An expired Authorization header does not block these endpoints.

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

Tokens use 32 cryptographically random bytes and expire after 30 minutes. Link tokens are stored only as SHA-256 hashes; OTPs are stored as salted password hashes in password_reset_tokens. Reset emails are sent after
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

## Request or resend the email OTP

POST /api/auth/password-otp

```json
{"token":"token-from-setup-or-reset-email"}
```

The frontend calls this after the user opens their setup/reset link and chooses
"Send code". It sends a six-digit code to the account's current employee email
(or email-form username for an account without a linked employee). The request
cannot override the recipient. It works for ACTIVE and INVITED accounts, including
onboarding invitations and admin-triggered login setup links.

Returns 200 with a success message and Cache-Control: no-store. SMTP delivery is
attempted after commit; success is not proof of inbox delivery. OTP emails use the
shared header/footer, are private (no notification subscribers), and are not stored
in the email notification queue. Neither the OTP nor its hash is returned by the API.

- Code expires in 10 minutes or at the link's expiry, whichever is sooner.
- Wait 60 seconds between sends. An early resend returns 429 with Retry-After.
- Maximum three sends per link (initial send plus two resends).
- A resend replaces the previous code without restoring the failed-attempt budget.
- Five incorrect submissions exhaust that link's OTP attempt budget. Obtain a new
  setup/reset link; resending on the exhausted link is rejected.
- Send counts and incorrect-attempt counts are persisted and protected by row locks.
- Additional send throttling: 20 requests per IP/hour using the existing per-instance limiter.
- Existing links issued before deployment also require a code; there is no legacy bypass.

## Set a new password

POST /api/auth/reset-password

```json
{"token":"token-from-email","otp":"012345","newPassword":"new-password"}
```

Success returns 200 with the message "Password reset successfully." and Cache-Control: no-store. Password policy matches change-password:
nonblank, at least 8 Unicode characters, no more than 72 UTF-8 bytes, different
from the current password. Validation errors return 400 with a message.
Invalid, expired, used or inactive-account reset links return 400:

```json
{"message":"This reset link is invalid or expired. Please request a new one."}
```

Send otp as a six-character string to preserve leading zeros. Verification and the
password change occur together in this endpoint; no separate verified flag is
accepted from the frontend. Missing OTP returns OTP_REQUIRED; an expired OTP returns
OTP_EXPIRED; an incorrect OTP returns INVALID_OTP. The fifth incorrect submission
returns OTP_ATTEMPTS_EXCEEDED. Exhausted send limits return OTP_SEND_LIMIT_EXCEEDED.
A correct OTP with an invalid new password does not consume the link; the user can
correct the password while the code remains valid.

Password update, consumption of all the user's reset tokens, refresh-token
revocation and session-version increment commit atomically. A user-row lock
serializes concurrent resets, login and refresh. Access tokens with an old session
version return 401 on subsequent requests. Pre-deployment tokens without a version
are treated as version zero and are also revoked by a reset. The normal
password-changed notification is queued in the same transaction.

The schema is created/updated by the current Hibernate ddl-auto=update setting.
Deployments with schema updates disabled must apply
[the OTP migration](../database/migrations/2026-10-03-password-email-otp.sql) once before deploying.
It adds OTP columns to the existing password_reset_tokens table. Do not reapply it
if Hibernate has already added those columns.

## Frontend rollout

The backend now requires otp in every setup/reset submission. Update the frontend
in the same rollout: open the link, request the code, show a six-digit input and a
60-second resend countdown, then submit token + otp + newPassword. Clear the code
input on resend. Respect server Retry-After and prompt for a new link when the
attempt/send budget is exhausted. Do not log or persist the token or OTP in browser
storage. The existing link-only frontend cannot complete setup/reset after deployment.
