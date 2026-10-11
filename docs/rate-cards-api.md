# Rate-card duplicate validation

POST /api/v1/rate-cards creates a new ID. Return 409 when an existing card matches
all six fields: clientId, positionTitleId, mainSkillId, locationId, seniorityId,
and effectiveFrom. The check applies regardless of status. Null location/seniority
matches null. Additional skills, currency, hourly rate and effectiveTo are not
part of the duplicate key.

Different effectiveFrom dates are allowed even if date ranges overlap.
EffectiveFrom is required. EffectiveTo is optional for all statuses and cannot
precede effectiveFrom. Invalid dates return 400.

PUT uses the same check. In-place edits exclude their own ID; changes that create
a successor ID still check against the original card. A successor must therefore
have a different matching key, such as a different effectiveFrom date.
Client-row locking prevents simultaneous API writes from inserting duplicates.
Existing data is not changed; no schema migration is required.

Rate-card writes publish system email and bell notifications for CREATED, NEW_RATE,
RETIRED (transition to INACTIVE), and other status/end-date updates (UPDATED).
Unchanged saves and rejected duplicates do not notify. DELETE is not retirement.
Emails include card ID, client, position, skills, location, seniority, currency/rate,
effective dates and actor; successor notifications also include the original ID/rate.
Recipients are active FULL-access admins plus RATE_CARD and ALL_NOTIFICATIONS
subscribers. Bell notifications require a matching active FULL portal user.
The actor is not excluded. Email delivery requires enabled/configured SMTP and runs
asynchronously after commit; bell rows roll back with the rate-card transaction.
The RATE_CARD subscription category is seeded at startup. Existing notification
APIs support category RATE_CARD and relatedRecordType RATE_CARD; the UI should link
that record type to its rate-card view using relatedRecordId.
