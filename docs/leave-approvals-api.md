# Manager and lead leave approvals

All endpoints require authentication. Approver identity comes from the logged-in employee. Leave requests capture the Level 1 and optional Level 2 approvers from the employee's leave policy assignment when submitted. Timesheet approvers are separate. A manager role alone grants no access.

| Method | Path | Purpose |
| --- | --- | --- |
| GET | `/api/leave-approvals/my-team/pending` | Requests awaiting my action |
| GET | `/api/leave-approvals/my-team/approved` | Finally approved team requests |
| GET | `/api/leave-approvals/my-team/rejected` | Rejected team requests |
| GET | `/api/leave-approvals/my-team/all` | All submitted team requests |
| GET | `/api/leave-approvals/{requestId}` | Details, days and approval history |
| POST | `/api/leave-approvals/{requestId}/approve` | Approve at my configured level |
| POST | `/api/leave-approvals/{requestId}/reject` | Reject at my configured level |

Approve body: `{ "comments": "Approved" }`. Comments are optional. Reject body: `{ "reason": "Project delivery conflict" }`; reason is required. Neither body accepts an approver ID.

Pending rows include `requestId`, `employeeId`, `employeeName`, `leaveType`, `fromDate`, `toDate`, `totalHours`, `submittedAt`, `currentAvailableBalance`, `approvalLevel`, and `status`. Detail also includes employee email, leave type ID and unit, reason, notes, `availableByYear`, daily scheduled and requested hours, and approval history.

Level 1 approval changes `SUBMITTED` to `LEVEL1_APPROVED`. If no Level 2 approver is configured, it changes directly to `APPROVED`. Level 2 approval changes `LEVEL1_APPROVED` to `APPROVED`. Either level may reject only while its action is pending. Approval and rejection history is appended. On final approval, current balance is checked again and `used` is increased in the balance unit. Unlimited leave has no numeric deduction. Requests and balance rows are locked during approval to prevent repeat decisions and deductions.

Apply the SQL in `database/migrations/2026-09-19-leave-approvals.sql` and `database/migrations/2026-09-19-leave-policy-approvers.sql` to existing MySQL databases before deployment. Populate Level 1 approvers on existing assignments; enforce `NOT NULL` after the backfill. Existing submitted requests with no approver snapshot need a reviewed historical backfill before approvers can access them. The application does not infer approvers from timesheet setup.
