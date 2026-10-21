# Leave types

Maps the existing MySQL `leave_types` table. No create/drop migration or DELETE
endpoint is provided. Authentication follows the existing /api/** security rules.

| Method | Path | Purpose |
| --- | --- | --- |
| POST | /api/v1/leave-types | Create (201 with Location) |
| GET | /api/v1/leave-types | List active and inactive types |
| GET | /api/v1/leave-types?status=ACTIVE | Optional status filter |
| GET | /api/v1/leave-types/{id} | Get one |
| PUT | /api/v1/leave-types/{id} | Update |
| PATCH | /api/v1/leave-types/{id}/status | Activate/deactivate |

Create/update body:

```json
{"code":"ANNUAL","name":"Annual leave","description":"Paid annual leave","unit":"DAYS","paid":true,"status":"ACTIVE"}
```

Required: code, name, unit, paid. Unit is HOURS or DAYS. Code is trimmed and
uppercased; duplicate codes are rejected case-insensitively, including codes of
inactive types. Limits: code 50, name 150, description 1000 characters.
Description is optional; blank descriptions are stored as null. Status defaults
to ACTIVE on creation; omission on update preserves its previous value.

Status body:

```json
{"status":"INACTIVE"}
```

Use ACTIVE to reactivate. Neither operation removes the row. Responses include
id, code, name, description, unit, paid, status, createdAt, createdBy, updatedAt,
updatedBy. Audit timestamps map to created_at and updated_at; audit user IDs come
from the authenticated principal. Clients cannot supply audit fields.

Invalid input returns 400, missing IDs return 404, duplicate codes return 409.
The existing live table's column types/lengths were not available in the repository;
the entity uses the columns requested and the lengths above. No live database SQL
was executed as part of this change.
