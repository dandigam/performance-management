# SOW and Milestones Report API

The report returns one row per milestone. A SOW without milestones returns one row with blank
milestone fields.

`sowNumber` maps to the existing SOW `csxProjectId`. `plannedHours` and `invoiceAmount` map to the
milestone's estimated hours and amount. `currency` is returned when all positions in the milestone
resolve to one rate-card currency; it is blank when no currency or multiple currencies apply.

## Definition

`GET /api/v1/reports/sows/definition`

Returns the allowlisted selectable columns, filters, operators, lookup sources, fixed options,
default visibility, and sorting capabilities for report code `SOW_MILESTONES`.

ID/name field pairs are provided for clients and business units. ID fields drive lookup filters;
name fields drive displayed and exported columns.

## Query

`POST /api/v1/reports/sows/query`

```json
{
  "columns": [
    "sowNumber",
    "sowName",
    "clientName",
    "businessUnitName",
    "sowStatus",
    "milestoneName",
    "milestoneStatus",
    "plannedHours",
    "positionCount",
    "invoiceAmount",
    "currency"
  ],
  "filters": [
    { "field": "sowStatus", "operator": "EQUALS", "value": "ACTIVE" }
  ],
  "sort": [
    { "field": "sowName", "direction": "ASC" }
  ],
  "page": 0,
  "size": 25
}
```

Summary values respect all current filters:

```json
{
  "totalSows": 12,
  "activeSows": 8,
  "totalMilestones": 34,
  "totalPlannedHours": 18420,
  "totalPositions": 46
}
```

Contract value is omitted because a single total would be incorrect when currencies differ.

## Excel export

`POST /api/v1/reports/sows/export`

Uses the same `columns`, `filters`, and `sort` properties as the query request, without pagination.
The workbook contains all matching rows and only the requested columns in the requested order.
The response filename is `sow-milestones-yyyy-MM-dd.xlsx`.
