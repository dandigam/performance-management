# Resend manager review notifications

Both endpoints require ADMIN and use the existing assigned reviewer, not the current
organization hierarchy. `stageId` is `employee_review_assessments.id` and must belong
to `reviewId`.

`GET /api/employee-reviews/{reviewId}/stages/{stageId}/notification` returns:

```json
{
  "reviewerName": "Srini N",
  "email": "corrected@example.com",
  "canResend": true,
  "unavailableReason": null
}
```

After admin confirmation, send
`POST /api/employee-reviews/{reviewId}/stages/{stageId}/notification/resend`:

```json
{"expectedEmail":"corrected@example.com"}
```

Success is `202 Accepted` with no body: queued, not confirmed delivered.
The server reads and locks the current reviewer email while queueing; the request
cannot override the recipient. Email comparison ignores case and surrounding spaces.
A changed address returns `409 REVIEWER_EMAIL_CHANGED`; refresh and confirm again.
Unavailable stages return `409 NOTIFICATION_UNAVAILABLE`. GET reports the reason.
Non-admin access returns 403; an assessment outside this review returns 404.

Resending is available only for incomplete manager stages whose previous assessments
are submitted, with an active reviewer and active login account, a valid current email,
an unexpired effective deadline, and email delivery enabled. Login setup must be
completed separately. Deadline resolution follows the existing explicit override,
matching timeline role, then matching stage order behavior.

Each resend creates a new ASSESSMENT_READY email queue entry using the existing template
and a separate resend deduplication key. Concurrent requests are serialized per assessment;
another resend within 60 seconds returns `429 RESEND_TOO_SOON` with Retry-After.
During that cooldown, GET returns `canResend: false` with the wait message.
The corrected email is captured when queued; later changes require a new confirmation.
This action neither retries the original wrong-address email nor changes its status.
It does not change assessments, reviewer assignments, deadlines, answers or ratings,
and does not generate an additional bell notification. No database migration is needed.
