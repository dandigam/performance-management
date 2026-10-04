# Employee login setup

Creating an active employee through `POST /api/employees` now creates an `INVITED`
login account and sends the existing branded account invitation email to the
employee's email address. The previous employee-created email is replaced by this
invitation. The admin employee-created notification remains unchanged.

No shared default password is assigned or returned. The database initially holds
a hash of a random, undisclosed value. The create response retains `employee`,
`userId`, `username`, and `roleName`; the `password` field has been removed.
The UI should show a password-setup instruction instead of displaying credentials.
Email delivery requires enabled, working SMTP; creating the employee does not
confirm inbox delivery.

The employee opens the single-use setup link (valid for 24 hours), requests an email
OTP, and submits the OTP with their chosen password. Successful setup activates the
login. See [the OTP API flow](password-reset-api.md).

Employee updates that create a missing login use the same invitation flow. Updating
an active employee's profile does not activate an existing `INVITED` login or resend
its invitation. Existing passwords are not changed by this update.

Inactive employees retain an `INACTIVE` login and do not receive a setup invitation.
After activation, an admin can send the setup email through
[`send-login-setup`](send-login-setup-api.md).
