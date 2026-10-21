# Holiday changes and DRAFT SOW position hours

Holidays are client-specific: adjustments only affect SOWs with the same client ID.
Unassigned legacy holidays do not adjust SOWs. See `holidays-api.md` for the required
clientId write field, rollout migration, and legacy-data reconciliation notes.

The existing `POST /api/v1/holidays`, `PUT /api/v1/holidays/{id}`, and
`DELETE /api/v1/holidays/{id}` APIs adjust planned milestone position hours in the
same transaction. No additional API call or scheduled job is needed. The client
calendar migration is required when adopting client-specific holidays.

An active weekday ONSITE holiday deducts 8 hours from each matching position when added
or activated. Deleting or deactivating it restores 8 hours. For example,
`160, 140, 40` becomes `152, 132, 32`; removing a holiday from positions currently
holding `152, 140, 20` produces `160, 148, 28`. Existing entered hours are assumed
to already reflect existing holidays; this is a delta adjustment, not a calendar
recalculation or historical backfill.

Separate methods subtract hours for an added holiday and restore hours for a
removed holiday. Each fetches and locks candidate DRAFT SOWs once using milestone
overlap with that holiday's calendar month, including year. An update first calls
restoration for the old date, then subtraction for the new date; identical dates
are skipped. For each candidate SOW, a filtered database query loads only matching
positions of any location, ordered by milestone ID and position ID. It checks the holiday
date against both milestone and position dates in the database, without loading
full milestone or position collections. Both hour-adjustment methods use this same
filtered fetch. Matching uses both the milestone dates
and any position date bounds, inclusively. Null position bounds inherit the
milestone bounds. Only ONSITE holidays participate; ONSHORE is treated as ONSITE.
The ONSITE holiday calendar applies to every matching position, including OFFSHORE
positions. OFFSHORE holidays do not trigger adjustments. Other SOW statuses are excluded.

This change does not repair hours previously skipped by the position-location filter.
Those existing positions need a separate targeted correction; toggling an existing
holiday off and on will not correct the missed deductions.

Changing a holiday's date or location restores the old applicable day and deducts
the new applicable day. A position matching both has no net change. Name/description
edits, identical repeated updates, inactive holidays, and Saturday/Sunday holidays
do not change hours. A weekday-to-weekend move restores the old weekday only.
Changing ONSITE to OFFSHORE restores the old ONSITE hours only; changing OFFSHORE
to ONSITE deducts the new ONSITE hours only.

Blank position hours remain unplanned and are skipped. Non-numeric hours or a
deduction below zero reject the entire request with 400, including the holiday
change. There is no silent clamp: clamping would make later restoration inaccurate.
Existing protection against deleting holidays referenced by timesheet records
remains; deactivation is available instead.

This updates `sow_milestone_positions.hours` and `amount`. Each position amount
changes by the signed hours adjustment multiplied by that row's `hourly_rate`
(minus or plus 8 times the rate), rounded to two decimals. If the amount is null,
the starting amount is derived from the existing hours times the rate. A missing
or negative rate, negative resulting amount, or amount exceeding the column's
precision rejects the whole holiday change. A zero rate is allowed.
Each affected milestone amount is refreshed from the sum of all its position
amounts using a database aggregate, without loading all positions. Existing
amount adjustments are preserved by applying a delta rather than replacing the
amount with hours times rate. Each affected milestone's `estimated_hours` is also
refreshed from all its position hours, including positions outside the holiday date
range. Only the hours column is fetched for this total, not full position entities.
Blank hours count as zero. Invalid or negative position hours, or a total that is
fractional or outside the integer range, reject the transaction because
`estimated_hours` is an integer column. For example, five positions changing from
80 to 72 hours update the milestone estimate from 400 to 360.
Timesheet schedules and actual entries are not rewritten. Earlier hours-only
adjustments are not backfilled.
Holiday rows and candidate SOW rows are locked to serialize holiday adjustments
and coordinate with existing SOW status/owner updates.
