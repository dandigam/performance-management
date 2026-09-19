# SOW assignment column removal

`EmployeeAssignment` contains only the employee, SOW, manager, lead, status and
effective dates, plus ID and audit fields. It has no formula or transient legacy
properties. Service response mapping keeps the existing DTO fields and structure.
Department comes from `sows.business_unit_id`.
Designation comes from the assigned milestone position; position type comes from
the milestone-position assignment. These last two parent summaries use active
child assignments and return null when the children have different values.
The employee assignments response still includes individual milestone details.
Milestone ID and allocation summaries also come from child assignments. Scalar
summaries are null if there is no unambiguous value. `isPrimaryAssignment` remains
in the response as null because this flag has no source in the simplified model.
Where an operation needs one current parent, it selects the newest active parent
by effective date and ID. Review designation uses the employee profile designation.

Set designation and position type through milestone-position assignment APIs.
Parent assignment APIs reject attempts to write those fields. Employee profile
designation remains independent. Department follows the SOW business unit.

Before removing obsolete parent-assignment columns from an existing database,
populate the milestone-position records that should replace any legacy parent
values. Legacy assignment data is not migrated automatically.
