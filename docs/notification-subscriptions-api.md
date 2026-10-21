# Notification subscription configuration

Both email delivery paths resolve additional recipients from active subscriptions at send time.
Existing employee and lead primary recipients are preserved.

Successful employee creation and updates through `/api/employees` send a separate
admin notification after transaction commit to active `ALL_NOTIFICATIONS` subscribers,
using their configured CC/BCC setting. These messages include employee name, employee
ID, email, and status; no passwords or invitation tokens are included. The employee's
creation welcome email remains separate. Mail must be enabled and the subscription
configured with admin addresses; without recipients, the admin notification is skipped.

All endpoints below require an authenticated ADMIN, including reads.
Base path: `/api/v1/notification-subscriptions`.

| Method | Path | Result |
| --- | --- | --- |
| GET | base path | List configurations, ordered by ID |
| GET | `/{id}` | Get configuration |
| POST | base path | Create (201) |
| PUT | `/{id}` | Replace configuration (200), including enable/disable |
| DELETE | `/{id}` | Delete configuration (204) |

POST/PUT body (all fields required):

```json
{
  "categoryId": 123,
  "emailAddresses": "hr@company.com,admin@company.com,finance-dl@company.com",
  "recipientType": "BCC",
  "active": true
}
```

Use the actual lookup value ID, not the example ID. Find the lookup type with code
`NOTIFICATION_CATEGORY` using `GET /api/lookup-types`, then populate the category dropdown
from `GET /api/lookup-types/{typeId}/values`. Offer active values for new active configurations.
`ONBOARDING` and `ALL_NOTIFICATIONS` are seeded on application startup without changing existing
values. Additional supported codes are `LEAVE`, `TIMESHEET`, `PERFORMANCE_REVIEW`, `SOW`,
`INVOICE`, and `RESOURCE_ALLOCATION`; create these using existing lookup CRUD.

One row per category, including inactive configurations. Edit/reactivate an existing row
instead of creating another. Recipients are stored comma-separated. Whitespace is trimmed
and duplicates removed case-insensitively, preserving the first address's spelling.
Individual and DL addresses use the same format; DL membership is managed by the mail system.
Empty entries, display names, malformed addresses, CR/LF, and lists over 10000 characters
are rejected. CC/BCC applies to the entire list; mixed copy types per category are not supported.

Responses contain `id`, `categoryId`, `categoryCode`, `categoryName`, `emailAddresses`,
`recipientType`, `active`, `createdBy`, `createdOn`, `updatedBy`, and `updatedOn`.
Duplicate categories return 409; missing records/categories return 404.
An inactive category cannot have a subscription created or updated as active;
its subscription can still be disabled or deleted.

Run `database/migrations/2026-09-29-notification-subscriptions.sql` using the existing
manual migration process. No real recipients are seeded.

UI source is not present in this repository (only compiled frontend assets). The frontend
should provide category, email list, CC/BCC selector, active toggle, list/edit/delete actions.
CC exposes copy addresses; BCC hides them.

## Delivery behavior

`NotificationRecipientResolver` combines the specific category with `ALL_NOTIFICATIONS`
(the global admin group). Missing/inactive categories, lookup types, or subscriptions add no
recipients and do not raise a missing-group exception. Primary recipients remain in To.
Deduplication is case-insensitive per email; To wins over copies and BCC wins over CC.
Separate personalized emails may each generate a subscriber copy.

SOW, invoice/payment, and onboarding submission notifications use subscription recipients
without a hardcoded primary admin. Configure these categories or `ALL_NOTIFICATIONS` before
deployment. The former `ADMIN_EMAIL` and `ONBOARDING_REVIEW_RECIPIENTS` settings are no longer
used. Onboarding submissions enqueue one category-only email with an empty primary address.
No recipients means no send; queued messages are marked `SKIPPED` without retries.
Apply `database/migrations/2026-09-29-notification-delivery-skipped.sql` before deployment.

Generic manual notifications and reminders use the global group. Password reset/change,
account creation, and account/onboarding invitations remain private and bypass subscriptions.
Database/SMTP failures still follow existing delivery failure handling; they are not treated
as missing configuration. This change does not add new workflow triggers.
