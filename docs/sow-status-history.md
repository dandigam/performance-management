# SOW status history

Existing API URLs, request payloads, and response bodies are unchanged. A read-only history API is available as described below.
The current status and effective date remain in `sows`.

Apply `database/migrations/2026-10-03-sow-status-history.sql` before deploying.
The optional `2026-10-03-sow-status-history-baseline.sql` migration adds one marked baseline for each existing SOW without history. New SOW creation records the initial status with a null
previous status. Each successful status PATCH records a transition in the same transaction.
The general SOW update already ignores workflow fields and produces no status history.
Invalid transitions produce no history. Status updates lock the SOW to serialize transitions.

History contains snapshot status codes, effective date, actual change timestamp, and authenticated
actor ID (null when no authenticated actor exists). `approved_at` is set only for an explicit
APPROVED status, including initial creation as APPROVED if allowed by the existing create API.
Direct activation does not imply a separate approval timestamp. Dates use the application's
existing server-local timestamp convention.

The table deliberately stores the SOW ID without a foreign key so the existing DELETE operation
continues to work and the audit trail survives deletion. No history rows are modified by APIs.

Baseline rows have is_baseline=true. Their changed_at is the snapshot time, not an inferred
transition time; changed_by and approved_at remain null. Missing effective dates remain null.
Normal application history rows have is_baseline=false. Main SOW records are not modified.


## Read history

GET /api/v1/sows/{sowId}/status-history returns a JSON array, oldest first by changedAt then id.
Uses the existing SOW authentication rules. No body or pagination parameters are required.
Fields: id, sowId, previousStatus, status, statusEffectiveDate, changedAt, changedBy,
changedByName, approvedAt, baseline. Actor names reflect current user details; missing users
or unknown actors have a null name. Baseline effective dates may be null.
Existing SOW without history: 200 with []. Missing or deleted SOW: 404; retained history
for deleted SOWs is not exposed by this endpoint.


Correction workflow: WAITING_FOR_APPROVAL may return to DRAFT, then be edited and
resubmitted to WAITING_FOR_APPROVAL. Both transitions are recorded; other transition rules remain unchanged.
