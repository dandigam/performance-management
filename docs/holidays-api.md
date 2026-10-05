# Client holiday calendars

Existing `/api/v1/holidays` endpoints and authentication/audit rules are retained.
POST and PUT `/{id}` require a positive `clientId` referencing `clients.id`:

```json
{
  "clientId": 2,
  "holidayName": "New Year's Day",
  "holidayDate": "2026-01-01",
  "locationType": "ONSITE",
  "description": "Client holiday",
  "active": true
}
```

Use IDs from the clients API, never hardcoded client names/IDs. Missing/non-positive
client IDs return 400, unknown clients return 404. Duplicate client + normalized
location + date returns 409, including inactive holidays. The same date is allowed
for different clients or locations. POST defaults active to true; PUT retains the
current active value when omitted. Other existing field behavior remains unchanged.

Create, update, detail and list responses include `clientId` and `clientName`.
Unassigned legacy rows have null client fields.

`GET /api/v1/holidays?clientId=2&year=2026&locationType=ONSITE&active=true`
returns only that client's matching holidays, excluding unassigned rows. A valid
client with no matches returns `[]`. Unknown clients return 404, non-positive IDs
return 400. Year defaults to the current year; location and active are optional.
During rollout, omitting clientId retains the cross-client list including legacy
unassigned rows. Results are ordered by holiday date, then ID.

ONSITE weekday holiday adjustments now affect only DRAFT SOWs linked to the holiday's
client. Position locations do not restrict eligibility. Separate add/remove methods
continue adjusting hours by 8, position amounts by 8 times hourly_rate, and milestone
totals. Changing client restores the old client's applicable hours and deducts the
new client's, even when the date stays the same. Unassigned holidays never trigger
SOW adjustments; explicitly assigning one applies the holiday to the selected client.
Existing manual position hours are still adjusted by delta rather than overwritten
with calendar defaults. Weekend and inactive holidays remain excluded.

Apply `database/migrations/2026-10-05-holiday-client.sql` once BEFORE deployment,
even with Hibernate auto-update, to remove the old global unique index. It adds
nullable client_id, its foreign key, and the composite unique lookup index. Do not
run it after Hibernate has already added client_id without adapting the migration
to that schema. It does not assign clients or repair earlier cross-client adjustments;
review historical hours separately before mapping legacy holidays to avoid double deductions.

Frontend code is outside this change: pass clientId for calendar/default-hours
requests and writes. Existing timesheet reference protections on deletion remain.

If POST returns the generic database-conflict message after Hibernate auto-update,
inspect `SHOW INDEX FROM holidays`. The obsolete `uk_holiday_location_date` index
can remain even after the client column and new index were created. For partially
migrated installations, use `database/migrations/2026-10-05-holiday-client-repair.sql`
instead of rerunning the original migration. Run it against the application database
and stop on any SQL error. It checks for existing schema objects and preserves data.
Also check for an actual duplicate with the same client, location, and date; the
generic handler maps all data-integrity failures to 409, so server logs are needed
to establish the exact failing constraint if the indexes are already correct.
