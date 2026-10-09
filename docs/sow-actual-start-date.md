# SOW actual work start date

`startDate` and `endDate` remain the planned SOW period. `actualStartDate` is a
separate, server-managed date returned by SOW detail/list/create/update/status/
signature responses and by each `/api/v1/sows/summaries` item.

The Start Work dialog uses the existing endpoint and request:

```http
PATCH /api/v1/sows/6/status
Content-Type: application/json
```

```json
{
  "status": "ACTIVE",
  "statusEffectiveDate": "2026-10-08"
}
```

When the SOW becomes ACTIVE and actualStartDate is unset, the server records the
selected statusEffectiveDate as actualStartDate. The date cannot be in the future.
It is not required to match the planned period: actual work can start early or late.
Creating a SOW directly as ACTIVE also records its effective date (today if omitted).
Existing status transition validation, audit history and notifications still apply.

Later status changes, reactivation and ordinary SOW edits preserve actualStartDate.
It is not accepted as an editable request field. The current statusEffectiveDate can
continue changing independently. No automatic position date/hour changes are made.

Apply `database/migrations/2026-10-08-sow-actual-start-date.sql` before deployment
when automatic schema updates are disabled. If Hibernate already added the column,
run only the UPDATE section. It fills missing dates from the first non-baseline
ACTIVE history entry, ordered by changed_at then id, without overwriting existing
actual dates. Apply the status-history migration first if that table is missing.
No planned date or baseline snapshot is used to guess an unknown start.

Legacy SOWs without trustworthy activation history remain null. If subsequently
activated with no actual date recorded, the newly selected date is captured; this
does not reconstruct an earlier unrecorded activation. Review those legacy records
before reactivation if their original work start must be retained.
