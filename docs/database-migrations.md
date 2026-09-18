# Database migrations

## Deploying the position-status migration

For the production deployment that introduces position statuses, set:

```
SOW_POSITION_STATUS_MIGRATION_ENABLED=true
```

The Java startup migrations run after Hibernate has updated the schema. They convert legacy
`ACTIVE` values to `ASSIGNED` in both `sow_employee_assignments` and
`sow_milestone_position_assignments`, then derive each position's status as `ASSIGNED`,
`CLOSED`, or `OPEN`.

Completion is recorded in the database table `application_data_migrations`. Subsequent
restarts and deployments skip the migration automatically. Take a database backup and
test it against a production copy before deployment.
