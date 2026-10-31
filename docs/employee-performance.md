# Employee performance summary

```http
GET /api/employees/3/performance
Authorization: Bearer <access-token>
```

No request body is required. The employee ID selects the subject, never the actor.
The server resolves the active authenticated account and its linked employee.

- ADMIN/HR may see all reviews and stage ratings for the subject, even when their
  own account has no linked employee.
- Employees may see their own assessment stages and published final ratings.
- Other callers must have a saved assessment assignment for the subject. They see
  only assigned reviews, their highest assigned stage, and submitted earlier stages,
  matching existing reviewer visibility. A manager title or current reporting
  relationship alone does not grant access to review history.
- Unrelated callers receive 403. Authorized self/Admin/HR requests for an employee
  without reviews return an empty performance list; missing employees return 404.

Success is a direct EmployeePerformanceResponse object with employeeId, employeeName
and performance. Each entry contains reviewId, cycleId, reviewName, status, stageRatings,
finalRating and publishedDate. Stages contain assessmentId, assessmentLevel, stageCode,
stageName, reviewerEmployeeId, reviewerName, status, overallRating, overallComment and
submittedDate. No question answers, personal profile or financial details are included.
Unpublished final ratings and dates are null for all callers. Responses use no-store.

This is a read-only endpoint. It sends no emails and changes no stored data. Existing
employee/review/rating endpoints have not had their access rules changed by this work;
their previously identified authorization gaps remain separate. No migration is needed.
