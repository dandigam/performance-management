# Production startup database initialization

Deploy with the `prod` profile and keep the existing external production properties
file and its credentials. Use `spring.jpa.hibernate.ddl-auto=update` so Hibernate
creates missing tables/columns from entities, including `employee_hierarchy_imports`.

The previous PERFORMANCE_STAGE, feature catalog, invoices-to-finance conversion and
role-feature flag initialization has completed in production and has been removed
from `ProductionReferenceDataInitializer`. Existing reference data and flags remain
in the database. A fresh database must have that reference data provisioned separately;
this initializer no longer seeds a fresh installation.

The current prod-only initializer checks `employees.joining_date` after Hibernate schema
update. If it is a non-nullable DATE column, it changes it to `DATE NULL`, preserving
all existing values. If already nullable, it performs no DDL. Missing columns or an
unexpected data type fail startup with an explicit message. The initializer does not
use a transaction because MySQL ALTER TABLE implicitly commits.

Employee hierarchy import creates missing users and current employee-role mappings
only when a batch is committed. It does not backfill existing employees at startup,
send invitations, create password-setup tokens, or change existing account permissions.
An active SYSTEM_ROLE / EMPLOYEE lookup must already exist for default account creation.
Reupload previously imported employees to provision missing accounts through a new batch.

Completed migration scripts have been removed. Schema updates are managed by Hibernate
and the prod-only initializer described above. Read-only diagnostics and the explicitly
named review-data cleanup script remain separate utilities; they are not run at startup.
Flyway remains removed; old schema-history entries are not read. Existing review data,
reviewer assignments, feature settings and employee joining dates are preserved.
Application file logging continues using logging.file.name or APP_LOG_FILE.

Build a fresh JAR with `mvn "-Dmaven.test.skip=true" package`. Tests have not been modified.
Production needs ALTER permission for the conditional joining-date adjustment and the
normal permissions required by Hibernate schema update. Schema changes can take a metadata
lock; schedule deployment appropriately for the database size and workload.
