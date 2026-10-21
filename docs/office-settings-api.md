# Office locations and General Settings

These APIs manage one company's shared portal name and any number of office locations.
With application authentication enabled, all GET requests require authentication;
POST and PUT require ADMIN. They follow the existing development authentication bypass
when app.security.authentication-required=false.

## Endpoints

| Method | Path | Result |
|---|---|---|
| GET | /api/v1/settings/general | 200: shared settings |
| PUT | /api/v1/settings/general | 200: updated settings |
| GET | /api/v1/settings/office-locations | 200: office array |
| GET | /api/v1/settings/office-locations/{id} | 200: one office |
| POST | /api/v1/settings/office-locations | 201: new office with Location header |
| PUT | /api/v1/settings/office-locations/{id} | 200: updated office |

List filters are optional: ?active=true&countryCode=US. Results are ordered by
country code, office name, then ID. Group by countryCode in the UI.

## Create or update an office

POST and PUT use the same full request. PUT replaces all editable fields; omitted
optional address/contact fields become null. active must be explicitly supplied.

```json
{
  "officeName": "Richardson Head Office",
  "addressLine1": "300 N Coit Road",
  "addressLine2": "Suite 340",
  "city": "Richardson",
  "stateRegion": "Texas",
  "postalCode": "75080",
  "countryCode": "US",
  "timeZone": "America/Chicago",
  "phone": "+1 904-767-1685",
  "email": "info@railinfotech.com",
  "active": true
}
```

Required: officeName, addressLine1, city, countryCode, timeZone, active.
Country codes are validated against ISO two-letter countries and stored uppercase.
Time zones must be supported named Java ZoneId values; numeric offsets are rejected.
Email is validated when supplied. Responses contain all fields above plus id,
mainOffice (boolean), createdOn and updatedOn. Audit actors are determined by the
application, not accepted in the request.

For the four offices shown, use separate rows for Richardson, Jacksonville,
Nanakramguda and Narapally. Suggested zone identifiers are America/Chicago for
Richardson, America/New_York for Jacksonville and Asia/Kolkata for Hyderabad.
This migration creates no office records; enter and confirm the addresses through
these APIs. It does not fabricate locations or overwrite company contact data.

## Shared settings and main office

GET returns this shape (mainOfficeId is null before an active office exists):

```json
{"portalName":"RailInfo Tech","mainOfficeId":1}
```

PUT /api/v1/settings/general accepts the same shape. portalName is required,
nonblank, and at most 150 characters. mainOfficeId must reference an active office.
It may be null only if no active offices exist. The first active office created or
activated is selected automatically. There is only one main-office pointer in the
singleton company_settings row (id=1); it is not a separately editable flag on each office.

To deactivate the main office, first select another active main office through
PUT /general, then PUT the old office with active=false. This preserves historical
references. No hard-delete endpoint is provided. Main-office updates and office
writes acquire the same database lock to prevent concurrent invalid selections.

Invalid input or deactivating the main office returns the existing 400 error
response. Unknown office IDs return 404. Unauthorized operations use existing
401/403 responses.

## Deployment and UI scope

Apply [office settings schema](../database/migrations/2026-10-03-office-settings.sql)
when Hibernate automatic schema updates are disabled. The application can also
initialize its default company_settings row on the first write (MySQL INSERT IGNORE).
Before that, GET /general returns RailInfo Tech and a null mainOfficeId without writing.

The UI must load portalName from GET /general to display the shared name. Office
zones are metadata for the UI and future scheduling integrations; this API does not
change server time, existing timestamps, email branding, payroll rules or employee
workLocation values. Assigning employees to office IDs is a separate integration.
