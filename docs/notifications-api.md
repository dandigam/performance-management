# In-app notifications

The bell inbox is independent of email delivery, email subscriptions and device-only
preferences. Alerts are stored per login account in `user_notifications`. Nothing is
backfilled from existing emails or historical workflow records.

All endpoints require a signed-in active user, even with the development authentication
bypass enabled. Recipient IDs cannot be supplied by the caller. Admins see only their
own inbox. Onboarding-only accounts may access these endpoints too.

| Method | Endpoint | Response |
| --- | --- | --- |
| GET | `/api/v1/notifications` | Spring page of notifications, newest first |
| GET | `/api/v1/notifications/unread-count` | `{"count":3}` |
| PATCH | `/api/v1/notifications/{id}/read` | Updated notification |
| PATCH | `/api/v1/notifications/read-all` | `{"updatedCount":3}` |

List parameters: `page=0`, `size=20` (1–100), `unreadOnly=false`, and optional
`category` (`LEAVE`, `TIMESHEET`, `ONBOARDING`, `PERFORMANCE_REVIEW`). Ordering is
`createdOn DESC, id DESC`. Page results include `content`, `totalElements`,
`totalPages`, `number`, and `size`. Invalid pagination/category returns 400.
PATCH requests require no body. Repeating read operations is safe and preserves
the original read timestamp. A missing or someone else's notification returns 404.
Read-all marks unread rows at the time of the update; it does not mark future alerts.

Example notification:

```json
{
  "id": 42,
  "category": "LEAVE",
  "eventType": "LEAVE_SUBMITTED",
  "title": "Leave approval needed",
  "message": "Jane Doe's leave request (2026-10-12 to 2026-10-13) is awaiting your approval. Open the request to review it.",
  "createdOn": "2026-10-04T12:00:00Z",
  "read": false,
  "readAt": null,
  "relatedRecordType": "LEAVE_REQUEST",
  "relatedRecordId": 123
}
```

## Workflow coverage

| Workflow event | Recipient |
| --- | --- |
| Leave submitted | Level 1 approver |
| Leave level 1 approved, with another level pending | Level 2 approver |
| Leave finally approved or rejected | Employee |
| Leave cancelled | Level 1 approver (only submitted requests can currently be cancelled) |
| Timesheet submitted | Level 1 approver |
| Timesheet level 1 approved | Level 2 approver |
| Timesheet finally approved or rejected | Employee |
| Onboarding submitted/resubmitted | Configured reviewers, or active HR accounts by default |
| Onboarding corrections requested | Employee |
| Performance cycle published | Employee assigned the review |
| First assessment ready at cycle publication | Assigned assessor, when different from the employee |
| Next performance assessment ready | Assigned assessor |
| Overdue assessment reopened/extended | Assigned assessor |
| Performance results published | Employee |

Configure `app.notifications.onboarding-reviewer-user-ids=12,34` to restrict onboarding
review alerts to specific user IDs. An empty value selects active HR accounts. Explicit
recipients must be active HR/ADMIN users with FULL portal access. Admins are not added
automatically. Empty reviewer results are logged. This is separate from the email
subscription configuration and does not expand distribution lists.

Recipients without an active linked user account are skipped and logged. No account
is created and no fallback notification is sent to all admins. Reopening an assessment
creates a bell alert regardless of the existing email `notifyAssignees` option.

Notifications are inserted synchronously in the originating database transaction.
A rollback removes both the workflow change and its alerts. Recipient/event keys
prevent duplicate alerts; resubmissions and reopening occurrences use distinct keys.
Email delivery and retry operations do not create additional bell alerts.

## UI integration

Poll unread count every 30–60 seconds while the document is visible. Refresh on focus
and fetch page zero when the bell opens; fetch subsequent pages on demand. Avoid
overlapping requests. Refresh the count after a read action and clear cached data on
logout/account changes. Render title/message as plain text.

The frontend owns routes; no unverified UI URL is emitted. Map record references:

| `relatedRecordType` | `relatedRecordId` |
| --- | --- |
| `LEAVE_REQUEST` | Leave request ID; use employee/approver destination as appropriate |
| `TIMESHEET` | Timesheet ID |
| `ONBOARDING` | Employee ID; reviewers open the HR record, employees open their own profile |
| `EMPLOYEE_REVIEW` | Employee review ID |

Destination APIs must continue enforcing record access. An alert may outlive a
request's pending state or the recipient's assignment; handle unavailable records
gracefully. Reading an alert does not approve or otherwise change its related record.

## Deployment and scope

Apply [notification schema](../database/migrations/2026-10-04-user-notifications.sql)
before deploying when Hibernate schema updates are disabled. Timestamps use UTC
instants. The schema adds one table with recipient/list/unread/deduplication indexes.

This release does not add reminders, overdue schedulers, onboarding approval,
SOW/invoice/allocation/vendor alerts, preferences, deletion/retention jobs, SSE or
WebSockets. Those are separate follow-up work. Frontend source is not present here;
the bell UI must be connected in its source project.
