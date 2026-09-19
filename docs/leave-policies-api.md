# Leave policy configuration API

Base path: `/api/v1/leave-policies`. Requests and responses use JSON. Authentication follows the application's existing `/api/**` security rules. Dates use `YYYY-MM-DD`; audit timestamps are JSON date-time values. The rule's `leaveTypeId` references an existing row in `leave_types`.

## Endpoints

| Method | Path | Purpose | Success |
| --- | --- | --- | --- |
| `POST` | `/api/v1/leave-policies` | Create a policy | `201 Created` and `Location` header |
| `PUT` | `/api/v1/leave-policies/{id}` | Update a policy | `200 OK` |
| `GET` | `/api/v1/leave-policies` | List policies | `200 OK` |
| `GET` | `/api/v1/leave-policies?status=ACTIVE` | List policies with a status filter | `200 OK` |
| `GET` | `/api/v1/leave-policies/{id}` | Get a policy with its rules | `200 OK` |
| `PATCH` | `/api/v1/leave-policies/{id}/status` | Activate or deactivate a policy | `200 OK` |
| `POST` | `/api/v1/leave-policies/{id}/rules` | Add a leave type rule | `201 Created` and `Location` header |
| `PUT` | `/api/v1/leave-policies/{policyId}/rules/{ruleId}` | Update a rule | `200 OK` |
| `PATCH` | `/api/v1/leave-policies/{policyId}/rules/{ruleId}/status` | Activate or deactivate a rule | `200 OK` |
| `DELETE` | `/api/v1/leave-policies/{policyId}/rules/{ruleId}` | Deactivate a rule | `204 No Content` |

`GET /api/v1/leave-policies` returns policies ordered by name, then ID. Its `rules` arrays are empty; use the ID endpoint to retrieve rules. The ID endpoint includes active and inactive rules ordered by rule ID. `PUT` and policy status `PATCH` also return the policy with its rules.

## Create or update a policy

The same body is used for `POST /api/v1/leave-policies` and `PUT /api/v1/leave-policies/{id}`:

```json
{
  "policyName": "USA full-time policy",
  "country": "USA",
  "employmentType": "FULL_TIME",
  "effectiveFrom": "2026-01-01",
  "effectiveTo": null,
  "status": "ACTIVE"
}
```

| Field | Required | Allowed values and behavior |
| --- | --- | --- |
| `policyName` | Yes | Nonblank string, at most 150 characters; surrounding spaces are removed |
| `country` | Yes | `USA` or `INDIA` |
| `employmentType` | Yes | `FULL_TIME` or `CONTRACT` |
| `effectiveFrom` | Yes | Date in `YYYY-MM-DD` format |
| `effectiveTo` | No | Date on or after `effectiveFrom`, or `null` |
| `status` | No | `ACTIVE` or `INACTIVE`; defaults to `ACTIVE` on create and stays unchanged when omitted on update |

`PUT` replaces the policy fields shown above. Omitting `effectiveTo` clears it. The policy's rules are managed through the rule endpoints.

Example policy response (`POST`, `PUT`, `GET` by ID, and policy status `PATCH`):

```json
{
  "id": 10,
  "policyName": "USA full-time policy",
  "country": "USA",
  "employmentType": "FULL_TIME",
  "effectiveFrom": "2026-01-01",
  "effectiveTo": null,
  "status": "ACTIVE",
  "createdAt": "2026-09-19T09:30:00",
  "createdBy": 42,
  "updatedAt": "2026-09-19T09:30:00",
  "updatedBy": 42,
  "rules": [
    {
      "id": 25,
      "leaveTypeId": 3,
      "leaveTypeCode": "ANNUAL",
      "leaveTypeName": "Annual leave",
      "entitlement": 20.00,
      "carryForward": true,
      "maxCarryForward": 5.00,
      "status": "ACTIVE",
      "createdAt": "2026-09-19T09:35:00",
      "createdBy": 42,
      "updatedAt": "2026-09-19T09:35:00",
      "updatedBy": 42
    }
  ]
}
```

The create response has `rules: []`. Audit user IDs can be `null` if no auditor is available. List responses are arrays of policy objects with `rules: []`.

## Activate or deactivate a policy

`PATCH /api/v1/leave-policies/{id}/status`:

```json
{ "status": "INACTIVE" }
```

Use `ACTIVE` to reactivate. This changes the policy status only; it does not change rule statuses.

## Add or update a rule

Use the same body for `POST /api/v1/leave-policies/{id}/rules` and `PUT /api/v1/leave-policies/{policyId}/rules/{ruleId}`:

```json
{
  "leaveTypeId": 3,
  "entitlement": 20.00,
  "carryForward": true,
  "maxCarryForward": 5.00,
  "status": "ACTIVE"
}
```

| Field | Required | Allowed values and behavior |
| --- | --- | --- |
| `leaveTypeId` | Yes | ID of an existing leave type |
| `entitlement` | No | Nonnegative decimal with up to 8 digits before and 2 after the decimal; `null` means unlimited |
| `carryForward` | Yes | `true` or `false` |
| `maxCarryForward` | No | Nonnegative decimal with the same precision; stored as `null` when `carryForward` is `false` |
| `status` | No | `ACTIVE` or `INACTIVE`; defaults to `ACTIVE` on create and stays unchanged when omitted on update |

A policy may contain each leave type only once, including when its existing rule is inactive. To use an inactive rule again, set its status to `ACTIVE`. `PUT` can change the rule's leave type, provided that the new type is not already assigned to that policy. It can also set `entitlement` to `null` for unlimited leave.

`POST` returns a rule object like the one in the policy response, with `201 Created` and a `Location` header pointing to `/api/v1/leave-policies/{id}/rules/{ruleId}`. `PUT` and rule status `PATCH` return the same rule response shape with `200 OK`.

## Activate, deactivate, or remove a rule

`PATCH /api/v1/leave-policies/{policyId}/rules/{ruleId}/status`:

```json
{ "status": "INACTIVE" }
```

Use `ACTIVE` to reactivate. `DELETE /api/v1/leave-policies/{policyId}/rules/{ruleId}` also sets the rule to `INACTIVE`. It keeps the database row and returns no response body.

## Errors

| Status | When | Example body |
| --- | --- | --- |
| `400 Bad Request` | Missing or invalid field, unsupported enum, malformed JSON, or `effectiveTo` before `effectiveFrom` | `{ "error": "effectiveTo must be on or after effectiveFrom." }` |
| `404 Not Found` | Policy, rule, or referenced leave type does not exist; rule belongs to another policy | `{ "error": "Leave policy not found: 10" }` |
| `409 Conflict` | Leave type is already assigned to the policy | `{ "error": "This leave type has already been added to the leave policy." }` |

Field validation errors return a JSON object keyed by field name, such as `{ "policyName": "must not be blank" }`. Clients do not send `id`, audit timestamps, or audit user IDs in request bodies.
