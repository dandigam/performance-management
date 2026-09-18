package com.rit.performance.migration;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Converts the parent employee-SOW assignment status used by employee summaries.
 */
@Service
@Slf4j
public class EmployeeAssignmentStatusMigration {

    private static final String MIGRATION_KEY = "V2_EMPLOYEE_ASSIGNMENT_STATUSES";

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public void migrate() {
        entityManager.createNativeQuery("""
                CREATE TABLE IF NOT EXISTS application_data_migrations (
                    migration_key VARCHAR(100) NOT NULL PRIMARY KEY,
                    completed_at DATETIME NULL
                ) ENGINE=InnoDB
                """).executeUpdate();

        entityManager.createNativeQuery("""
                INSERT IGNORE INTO application_data_migrations (migration_key)
                VALUES (:migrationKey)
                """).setParameter("migrationKey", MIGRATION_KEY).executeUpdate();

        List<?> completionRows = entityManager.createNativeQuery("""
                SELECT completed_at
                FROM application_data_migrations
                WHERE migration_key = :migrationKey
                FOR UPDATE
                """).setParameter("migrationKey", MIGRATION_KEY).getResultList();

        if (completionRows.isEmpty() || completionRows.get(0) != null) {
            log.info("Employee assignment-status migration has already completed; skipping it.");
            return;
        }

        int assignmentsUpdated = entityManager.createNativeQuery("""
                UPDATE sow_employee_assignments
                SET status = 'ASSIGNED'
                WHERE UPPER(TRIM(status)) = 'ACTIVE'
                """).executeUpdate();

        entityManager.createNativeQuery("""
                UPDATE application_data_migrations
                SET completed_at = CURRENT_TIMESTAMP
                WHERE migration_key = :migrationKey
                """).setParameter("migrationKey", MIGRATION_KEY).executeUpdate();

        log.info("Completed employee assignment-status migration: {} assignments updated.",
                assignmentsUpdated);
    }
}
