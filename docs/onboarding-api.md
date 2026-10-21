# Create onboarding invitation

## HR queue and review

Onboarding responses include top-level `employmentType` (for example `FULL_TIME` or
`CONTRACT`), `roleName` (the employee's linked user-role display name), and `workLocation`
(the stored lookup code). Missing role/user or employment/location values return null.
For UI compatibility, `details.bankCurrency` mirrors `details.currency`, and top-level
`reviewComments` mirrors `review.comments`. Existing fields remain available. Bank fields
are included when a primary active employee bank account exists; reviewComments is null
when there is no review.

Successful submission and resubmission queue an ONBOARDING_SUBMITTED email to
`app.mail.onboarding-review-recipients`, defaulting to `venkatd099@gmail.com`.
Override with `ONBOARDING_REVIEW_RECIPIENTS` (comma-separated addresses). The email
uses the existing branded notification template and dispatcher, includes employee
name/number and submission time, and links to login. No frontend review route is
assumed. Resubmissions are labeled; repeated Submit calls while SUBMITTED do not
queue another email. The notification is persisted in the submission transaction.
Apply `database/migrations/2026-09-29-onboarding-submission-email.sql` if Hibernate
has not updated the MySQL event enum. SMTP delivery occurs after commit; submission
success means queued, not delivered. No historical submissions are emailed automatically.

Both GET endpoints require HR or Admin with access to HR APIs.
`GET /api/v1/hr/onboarding?page=0&size=20` returns a page with `content`,
`totalElements`, `totalPages`, `number` and `size`. By default it includes all
onboarding statuses, including INVITED, IN_PROGRESS and SUBMITTED, newest first.
Use `?status=INVITED` or `?status=SUBMITTED` for a filtered queue. Status is
case-insensitive; page starts at zero and size must be 1–100. Invalid values return 422.
Each row includes employee identity, designation, status, completion, invitation delivery,
submittedAt and version. Detailed personal and bank fields are omitted from queue rows.
`GET /api/v1/hr/onboarding/{employeeId}` returns the saved onboarding summary and
details for that employee (same structure as the self GET). Missing records return 404.
Both endpoints are read-only; they do not submit, approve or activate employees.

## Invitation creation

### Request changes

HR/Admin can POST `/api/v1/hr/onboarding/{employeeId}/request-changes` with
`{"version":4,"comments":"Please correct your employment dates."}`. Compensation
is not required. Use the version returned by the latest review GET, not a fixed value.
The record is locked and must be SUBMITTED with a matching version; stale versions
return 409 ONBOARDING_VERSION_CONFLICT and other stages 409 ONBOARDING_NOT_SUBMITTED.
Missing onboarding returns 404; missing/blank comments, comments over 5000 characters,
or missing/negative version return 422.
The transaction saves CHANGES_REQUESTED, review comments, the authenticated reviewer's
username and timestamp, increments the JPA version, and persists an employee email in
the existing notification queue. Returns 200 with the updated review record.
HR and employee GET responses expose `review: {comments, reviewedBy, reviewedAt}`.
Previous review comments remain visible when the employee edits and resubmits.
Employee status and portal access are unchanged. Queue filtering supports CHANGES_REQUESTED.
The existing mail dispatcher sends the comments and login link after commit; SMTP and
`app.mail.enabled=true` must be configured. API success means queued, not confirmed delivery.
Apply `database/migrations/2026-09-29-onboarding-review.sql` once if the schema has not
already been updated by Hibernate. No migration is executed automatically by this change.

`POST /api/v1/hr/onboarding` requires an authenticated HR or Admin, JSON content,
and a UUID `Idempotency-Key` header. The request fields follow `OnboardingCreateRequest`.
Email is limited to 50 characters by the existing login username column.
Role/designation IDs and employment type/work mode/work location codes must be active lookups.
CONTRACT employment requires an existing vendor ID. Past joining dates are permitted.

Creation returns HTTP 201 and the employee ID/number/name, email, designation name,
joining date, `INVITED` status, empty completedSections, the six required sections,
`QUEUED` invitation status, server-generated 24-hour invitation expiry,
null submittedAt and version 1.

The transaction creates a PENDING employee, INVITED user with ONBOARDING_ONLY access,
onboarding record, hashed single-use setup token and persisted email notification.
Existing employee detail tables remain the source of truth; no address, banking,
document or compensation placeholders are created. Employee activation is not performed.
The existing password reset endpoint consumes the setup link and enables login without
changing employee status or portal access. Restricted users can use authentication
endpoints, but cannot use regular business APIs (including the HR creation endpoint).
The restricted-account filter permits GET `/api/v1/onboarding/me`, PUT
`/api/v1/onboarding/me/{section}` for personal, address, education, employment-history,
bank-details and documents, and POST `/api/v1/onboarding/me/submit`.
GET `/api/v1/onboarding/me` is implemented and returns the authenticated employee's
current onboarding record (200), including live invitation delivery status. It resolves
the employee from the session, never a caller-supplied employee ID, and returns
404 `ONBOARDING_NOT_FOUND` when that employee has no onboarding record. It does not
change employee status or portal access, or expose password setup tokens.
PUT `/api/v1/onboarding/me/personal` saves phoneNumber, gender and dateOfBirth
directly to the existing employees row resolved from the authenticated session.
All three fields are required: phone is 7–15 digits with an optional leading plus,
gender is MALE, FEMALE, OTHER or PREFER_NOT_TO_SAY, and dateOfBirth must be in the past.
It returns the updated onboarding summary (200), validates input (422), and rejects
duplicate phone numbers or edits after submission/approval (409). The first save
changes INVITED to IN_PROGRESS; employee status and account access do not change.
GET and PUT responses derive personal section completion from these employee fields.
PUT `/api/v1/onboarding/me/address` accepts addressLine1, optional addressLine2, city,
state, postalCode and country. Required fields cannot be blank; limits match the existing
employee address DTO (200 characters per address line, 100 for city/state/country,
20 for postal code). Values are trimmed; missing or blank addressLine2 clears that field.
It creates or updates the signed-in employee's existing employee_addresses row and
returns the onboarding summary (200) with address completion derived from saved fields.
It uses the same editable statuses as personal updates and returns 422 for invalid
input, 404 for a missing onboarding record, and 409 for a non-editable onboarding.
No new table or migration is needed. Repeated saves reuse the same address row.
PUT `/api/v1/onboarding/me/education` accepts an educationDetails array containing
degree, institution, passingYear and percentage. At least one entry is required.
Degree and institution must be nonblank (maximum 100 and 250 characters); passingYear
is an integer from 1900 to 9999; percentage is 0 to 100 with up to two decimal places.
It atomically replaces the signed-in employee's list in employee_educations, mapping
degree to educationType and institution to collegeUniversity. No new table is needed.
It returns the onboarding summary (200), including education completion. Repeated
saves replace the list without accumulating duplicates. INVITED becomes IN_PROGRESS;
employee status and portal access are unchanged. Invalid input returns 422, missing
onboarding returns 404, and non-editable onboarding returns 409, as for address updates.
PUT `/api/v1/onboarding/me/employment-history` accepts a nonempty experienceDetails
array with companyName, position, location, fromDate and optional endDate (null or omitted
for ongoing employment). Text fields are required, trimmed and limited to 255 characters.
Dates use YYYY-MM-DD; endDate must be on or after fromDate. Invalid input returns 422
INVALID_EMPLOYMENT_HISTORY. The request atomically replaces only the signed-in employee's
list in employee_experiences and returns the onboarding summary (200), with
employment-history completed. INVITED becomes IN_PROGRESS; employee status and portal
access remain unchanged. Missing onboarding returns 404; non-editable stages return 409.
No new table or migration is needed.
PUT `/api/v1/onboarding/me/bank-details` accepts bankCountry (India or USA), currency
(three letters), accountHolderName, bankName, accountNumber (4–100 characters), and
bankCode. All fields are required. bankCode maps to IFSC for India or a nine-digit
routing number for USA. This follows the existing employee bank storage convention.
The signed-in employee's active primary bank_account is created or updated under the
onboarding lock. No new table is required. The response is the onboarding summary (200);
GET and PUT include the six saved fields under details and derive bank-details completion
from the saved account. INVITED becomes IN_PROGRESS; employee status and portal access
remain unchanged. Invalid input returns 422, missing onboarding 404, and locked stages 409.
POST `/api/v1/onboarding/me/submit` needs no request body. It resolves the employee
from the authenticated session and locks that employee's onboarding record. It checks
the stored requiredSections against completion derived from saved details; documents
remain optional under the current completion rules. Missing sections return 422
ONBOARDING_INCOMPLETE with the missing section names in the message.
INVITED, IN_PROGRESS and CHANGES_REQUESTED can transition to SUBMITTED; submittedAt
is recorded and the updated summary is returned (200). Repeated submissions while
SUBMITTED return 200 without changing submittedAt. Other stages return 409, and a
missing onboarding record returns 404. Employee status and portal access do not change.
Generic `/api/v1/documents` operations remain blocked for restricted accounts until
onboarding document handlers enforce ownership on upload, association, read and download.
Accepting a document ID in the documents section must also verify that ownership.

Idempotency is scoped to the authenticated username and UUID. An identical parsed
request replays the saved original 201 body, including the original QUEUED status.
A different request with the same key returns 409 IDEMPOTENCY_CONFLICT.
Different keys targeting an existing employee email/account return 409 EMPLOYEE_EMAIL_EXISTS.
Invalid fields/lookups or a missing/malformed key return 422. Unauthorized roles return 403.
The database unique constraint serializes concurrent identical-key creations, and all
records roll back together on failure. No separate idempotency table is used.

Apply `database/migrations/2026-09-27-employee-onboarding.sql` after the existing
portal-access migration and before starting the updated application. It retains existing
portal-access values and adds ONBOARDING_ONLY. Do not apply CREATE TABLE after Hibernate
has already created that table automatically. No migration was run by this implementation.

Enable the existing email dispatcher with `app.mail.enabled=true` and configure SMTP and
`app.mail.base-url`. Internal PENDING delivery means API QUEUED; sending later records
SENT or FAILED without removing the pending employee. The stored original creation response
does not change after delivery. Resend, review and activation APIs
are outside API 1. A future resend must invalidate the previous token and queue a new link.

Invitation email bodies contain the bearer setup link, so treat queue storage and email
administration access as sensitive; the token table itself stores only a hash.

## Password setup verification

Onboarding setup links require email OTP verification before the password is saved.
The frontend calls `POST /api/auth/password-otp` with the invitation token, then
submits `token`, `otp`, and `newPassword` to `POST /api/auth/reset-password`.
See [password setup/reset OTP flow](password-reset-api.md) for expiry, resend limits,
error handling, and the database migration.
