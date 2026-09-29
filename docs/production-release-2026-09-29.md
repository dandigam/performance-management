# September 29 production release

Production target and already-applied migrations must be established before execution.
This working tree also includes onboarding, user access, reports, and authentication changes;
the notification migrations alone do not deploy all of those changes.

## Database order

Take a recoverable database backup and stop application writers/mail workers for the migration
window. Inspect production schema and migration history; run only unapplied migrations.
Several older migrations are not repeatable. MySQL DDL implicitly commits, so transaction
rollback does not undo schema changes. Rehearse on a recent production clone first.

Use [`2026-09-29-production-consolidated.sql`](../database/migrations/2026-09-29-production-consolidated.sql)
as the single script for this release. It requires MySQL 8.0.16+, an explicitly selected
application database, CREATE ROUTINE / ALTER ROUTINE privileges, and a client supporting
`DELIMITER`. Stop on the first error; do not use `mysql --force`.

The baseline is the existing application schema with September 19-24 migrations applied.
The script checks normalized employee duplicates before mutations, skips existing columns
and equivalent employee unique indexes, expands portal access, creates onboarding and
subscription tables, updates email enums, and seeds all eight notification categories.
Matching email enum definitions are skipped on reruns. The portal-access check is recreated.
Existing lookup IDs, names, and active flags are preserved; recipient addresses are not seeded.

The consolidated script replaces the following individual migrations. Do not run these
afterward: earlier enum definitions can remove values required by this release.

1. `2026-09-26-employee-email-phone-unique.sql` (resolve normalized duplicate emails/phones first).
2. `2026-09-27-user-portal-access.sql`.
3. `2026-09-27-employee-onboarding.sql` (depends on the preceding portal-access migration).
4. `2026-09-29-onboarding-review.sql`.
5. `2026-09-29-onboarding-submission-email.sql` (must follow review; earlier scripts omit this enum value).
6. `2026-09-29-notification-category-lookups.sql`.
7. `2026-09-29-notification-subscriptions.sql`.
8. `2026-09-29-notification-delivery-skipped.sql`.

This list covers the pending September 26–29 SQL files, not a verified baseline-to-production
schema diff. Confirm earlier migrations and other entity changes against production.
The category seed inserts missing codes without overwriting existing lookup IDs or settings.
Creating a table IF NOT EXISTS does not repair an existing incompatible table.

## Schema comparison and validation

The working-tree entity changes are employee email/phone uniqueness, `users.portal_access`,
the new `employee_onboardings` entity (including review fields), and the new
`notification_subscriptions` entity. The consolidated script covers these and the updated
`EmailEventType` / `EmailDeliveryStatus` values. No additional missing columns were found
in this comparison. Response fields such as role name, work location, employment type,
and bank currency use existing data; review comments use the onboarding column added here.

Validation is static only; the consolidated script has not been executed against MySQL.
Before production, rehearse both initial application and a second run on a recent clone,
including any individually applied release migrations. Afterward, verify the resulting
columns, employee unique indexes, and all eight notification category lookups.

## Application rollout

Build the complete release with tests. Configure real production recipients through the
subscription API/UI, including `ALL_NOTIFICATIONS` for global admin copies. Never copy
development email addresses automatically. Existing inactive lookup values remain inactive.
The old ADMIN_EMAIL and ONBOARDING_REVIEW_RECIPIENTS settings no longer supply recipients.
Keep mail disabled until recipients are configured and verified; empty groups are skipped.

Deploy the tested artifact to the confirmed target, then check authentication, onboarding,
subscription CRUD permissions, and a controlled category email including CC/BCC visibility.
Enable mail after verification. Confirm queue status and errors.

`scripts/deploy-azure.ps1` currently skips tests and invokes azure-webapp:deploy; confirm its
target/plugin configuration before using it. Do not assume it identifies production.

## Rollback

Keep the previous artifact and database backup. Older application enums may not recognize
new SKIPPED statuses or onboarding event values. Review/handle these records before reverting
the application; do not blindly shrink enums or drop populated tables. Restore from the
validated backup only with an agreed recovery point and handling for intervening writes.
