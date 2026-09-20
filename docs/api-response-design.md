# API Message Response Design

All errors and message-only success responses use the same envelope.

## Expected business warning

```json
{
  "type": "WARNING",
  "code": "POSITION_HAS_RESOURCE",
  "message": "Position can't be removed. Unassign the resource first."
}
```

## Unexpected server error

```json
{
  "type": "ERROR",
  "code": "INTERNAL_ERROR",
  "message": "Unable to remove the position. Please try again."
}
```

## Successful message-only operation

```json
{
  "type": "SUCCESS",
  "message": "Position removed successfully."
}
```

`code` is stable and intended for client-side behavior. `message` is display text. Normal API
responses that return business data continue to use their endpoint-specific response DTOs.

## Work mode lookup

Employee `workMode` values are configured under the active `WORK_MODE` lookup type. Initial values
are `OFFSHORE` and `ONSITE`. Employee create, update, and summary filtering reject values that are
not active in this lookup.

Employee `workLocation` values are configured under the active `WORK_LOCATION` lookup type.
Initial values are `REMOTE`, `OFFICE`, and `HYBRID`. Employee summaries accept the optional
`workLocation` query parameter, for example `workLocation=HYBRID`.
The employee table stores the lookup code directly as text; no Java enum is used.

Employee `status` values are configured under the active `EMPLOYEE_STATUS` lookup type. Initial
values are `ACTIVE` and `INACTIVE`. Employee create, update, and summary filtering validate against
this lookup.
