# SOW milestone display order

Milestone objects accept and return an optional integer `displayOrder`, stored in `sow_milestones.display_order`.

- SOW create and update: set `milestones[].displayOrder`.
- Individual milestone update: set `displayOrder` in the request body.
- SOW responses, milestone update responses, paginated milestone summaries, and milestones returned for a position or resource requirement include `displayOrder`.

Example field: `"displayOrder": 1`.

Existing rows and new milestones without a value have null display order. Omitting the field or sending null on update preserves the existing value. List sorting is unchanged.

For databases managed with SQL migrations, apply `database/migrations/2026-09-24-sow-milestone-display-order.sql` once before deploying.
