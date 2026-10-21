# Delete a SOW milestone

DELETE /api/v1/sows/{sowId}/milestones/{milestoneId}

No request body.

Success: HTTP 200

```json
{
  "type": "SUCCESS",
  "message": "Milestone deleted successfully."
}
```

If any position is assigned: HTTP 400

```json
{
  "type": "WARNING",
  "code": "MILESTONE_HAS_ASSIGNED_POSITIONS",
  "message": "Milestone cannot be deleted. Please unassign all assigned positions first."
}
```

A missing milestone or a milestone belonging to another SOW returns HTTP 404.

Deletion removes the milestone, its positions, and inactive position assignment rows, then rebuilds the SOW resource requirements. Active assignments block deletion regardless of assignment dates.

Linked features, invoices, or timesheet records also block deletion (HTTP 400 with MILESTONE_HAS_FEATURES, MILESTONE_HAS_INVOICE, or MILESTONE_HAS_TIMESHEETS). Unassigning positions does not remove those linked records.
