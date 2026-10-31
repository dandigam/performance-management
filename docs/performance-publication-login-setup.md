# Login setup during performance publication

Explicit cycle publication checks every selected employee's linked portal account.

- ACTIVE: no setup invitation is sent; new reviews receive the normal performance email.
- INVITED without a valid setup token: the existing setup service creates a 24-hour
  token and publication sends one combined performance/setup email with one setup link.
  It does not send a separate generic invitation or normal performance email.
- INVITED with an unused, unexpired setup token: the existing link stays valid. New
  reviews receive the normal performance email; no replacement setup link is generated.
- Missing account or any other status: publication fails with an employee-specific
  INVALID_OPERATION message so HR can resolve the account. Accounts are not created,
  activated, or otherwise updated by this check.

Checks and invitations participate in the publication transaction. If any selected
employee fails validation, the entire publication rolls back, including setup tokens.
Existing email listeners send only after successful commit. No emails are sent merely
by saving cycle settings.

The account lock serializes token checks with explicit login setup and password reset.
Repeated publication does not generate another setup invitation while a valid token
exists. An expired or consumed token permits a new invitation for an INVITED account.
Tokens prove setup was generated, not that email delivery succeeded. They do not track
the recipient address; after correcting an email, HR should explicitly send login setup
to replace any valid token that might have been emailed to the old address.

Republishing checks existing selected employees too, but does not resend their normal
performance email, repair missing assessments, or change saved reviews. Existing setup
tokens and review history are preserved unless an invitation must be generated through
the existing setup service. No API request changes or database migration are required.
The publication response's emailsScheduled field counts both normal and combined
performance emails. A republish that recovers a missed invitation sends the combined
email even for an existing review. Mail delivery still requires app.mail.enabled.

This behavior applies to selected employee subjects only. Automatic setup for manager
assignees is outside this change. HR's explicit send-login-setup action remains available.
