# SOW summary client fields

`GET /api/v1/sows/summaries?page=0&size=20` includes `clientId` and `clientName`
in each item of `content`, alongside the existing summary fields. Values come from
the SOW's linked client; both are null when no client is assigned.

Example fields within a summary item:

```json
{
  "sowId": 101,
  "sowName": "Example SOW",
  "clientId": 2,
  "clientName": "CORYS"
}
```

Client IDs and names are read from the database, not hardcoded. Pagination and
authentication are unchanged. No database migration is required.
