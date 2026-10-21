# Send login setup email

`POST /api/v1/user-management/users/{userId}/send-login-setup`

Requires an authenticated `ADMIN`. Use the user's ID, not the employee's ID.
No request body is required.

```http
POST /api/v1/user-management/users/6/send-login-setup
Authorization: Bearer <admin-access-token>
```

Returns `204 No Content` after creating a login setup token and triggering email delivery
after the transaction commits. This response does not confirm inbox delivery.
Email delivery requires `MAIL_ENABLED=true` and working SMTP settings.

- Supports `INVITED` and `ACTIVE` users; other statuses are rejected.
- Sends to the linked employee's email. For users without a linked employee, sends
  to the email-form username used when creating the account.
- Uses the existing branded account invitation email and frontend
  `/reset-password?token=...` page.
- The link expires after 24 hours and can be used once. Resending invalidates all
  previous password setup/reset links for that user.
- Sending the email does not change the current password, account status, role, or
  portal access. Completing setup activates an `INVITED` account through the existing
  password reset flow.
- Returns `404` for an unknown user. Invalid account status or a missing recipient
  uses the application's existing invalid-operation error response.

The raw setup token is sent only in the email, not in the API response.

After opening the link, the frontend must request an email OTP using
`POST /api/auth/password-otp`, then submit `token`, `otp`, and `newPassword` to
`POST /api/auth/reset-password`. See [OTP flow and limits](password-reset-api.md).
The setup link alone no longer authorizes a password change.
