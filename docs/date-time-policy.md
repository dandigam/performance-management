# API date and time policy

The previous implementation used the JVM default zone for today's date and most
local timestamps. The deployed JVM's named zone cannot be determined from source;
an offset such as -04:00 alone does not identify it.

Set `BUSINESS_TIME_ZONE` to an IANA zone; the default is `America/New_York`.
The shared Clock supplies business dates across APIs, including Bean Validation
date constraints. Eastern daylight saving changes are handled by the zone rules.
Date-only values remain `YYYY-MM-DD`; do not convert them through browser UTC dates.

`GET /api/v1/date-context` follows existing API authentication rules and disables
caching. Example at the India/US boundary:

```json
{"businessTimeZone":"America/New_York","currentBusinessDate":"2026-10-09","currentInstant":"2026-10-10T02:00:00Z"}
```

India is already October 10 at that instant. Use `currentBusinessDate` for frontend
today/future-date checks; refresh on form open and after a date validation error.

Send for approval using `PATCH /api/v1/sows/{id}/status`:

```json
{"status":"WAITING_FOR_APPROVAL"}
```

The backend sets `statusEffectiveDate` to the current business date for every
status transition. Send for approval also
sets `submittedDate` to that date. Clients can omit `statusEffectiveDate`.
Activation takes effect immediately; the first activation uses the UI-supplied
`statusEffectiveDate` as `actualStartDate`, allowing future dates and defaulting to
today when omitted. The response's `statusEffectiveDate` still contains today's
business date. Other transitions ignore the supplied date, and reactivation preserves
the first actualStartDate. This endpoint does not schedule future activation.
General SOW writes no longer overwrite the workflow-managed submission date.

New audit timestamps use UTC: JPA auditing, manually recorded events and native
audit inserts. Hibernate JDBC time zone is UTC. Existing LocalDateTime response
fields retain their format and represent UTC; Instant fields include `Z`.
Existing historical local timestamps are not rewritten: determine their original
zone and arrange a separate migration before interpreting historical values as UTC.
Deploy/restart the backend to apply this policy. No database schema change is needed.

## JDBC conversion correction

`hibernate.type.java_time_use_direct_jdbc=true` binds Java date/time values directly,
avoiding the intermediate JVM-local `java.sql.Timestamp` conversion. With an Eastern
JVM and UTC JDBC calendar, that intermediate conversion added four hours in October.
The MySQL connection uses `connectionTimeZone=+00:00` and
`forceConnectionTimeZoneToSession=true`, keeping TIMESTAMP columns and session time
in UTC as well. Keep these settings together; the business clock remains Eastern.

A local MySQL temporary-table check reproduced `10:16` being stored as `14:16`
with the old binding. Direct binding stored and read back `10:16` for DATETIME and
TIMESTAMP, and preserved the DATE value. No existing records were changed.
Restart the backend to rebuild the connection pool and Hibernate mappings. Verify
a new update against UTC_TIMESTAMP(); existing shifted records require a separately
reviewed correction, not a blanket offset applied to all historical timestamps.
