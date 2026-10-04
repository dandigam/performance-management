# Asynchronous email delivery

Direct application emails now run on the dedicated `emailTaskExecutor` after a
successful transaction commit. The API response waits for the database work and
email task submission, not SMTP delivery. Rolled-back transactions do not send email.
Template rendering still occurs during the service operation.

This covers employee and SOW notifications, account invitations, reset links,
OTPs, and other `ApplicationEmail` events. Events contain rendered strings rather
than managed entities; subscriber resolution runs in its own transaction in the worker.
Existing persisted `EmailNotification` messages still use the scheduled dispatcher
and its existing retry behavior.

Optional application properties:

```properties
app.mail.async.core-pool-size=2
app.mail.async.max-pool-size=4
app.mail.async.queue-capacity=200
app.mail.async.shutdown-wait-seconds=30
```

SMTP connection/read/write timeouts remain configured at five seconds each.
Graceful shutdown allows queued tasks to finish and waits up to the configured
shutdown timeout. Delivery ordering is not guaranteed between workers.

The direct-email queue is bounded and in memory. Process crashes can lose queued
messages; SMTP failures are logged and are not retried automatically. If the queue
is full or shutting down, submission is logged as an error and the email is dropped;
it never falls back to sending on the API thread. Monitor these errors and size the
pool/queue for expected traffic. A successful API response does not confirm delivery.
Users can request another setup/reset email or resend the OTP subject to its limits.
No email bodies, tokens, codes, recipients, or task objects are logged on rejection.
