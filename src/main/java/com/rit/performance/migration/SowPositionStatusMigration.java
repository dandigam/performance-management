package com.rit.performance.migration;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Converts legacy SOW staffing statuses after the position-status field is added.
 * A database marker makes the migration execute only once per database.
 */
@Service
@Slf4j
public class SowPositionStatusMigration {

    private static final String MIGRATION_KEY = "V1_SOW_POSITION_STATUSES";

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
            log.info("SOW position-status migration has already completed; skipping it.");
            return;
        }

        int assignmentsUpdated = entityManager.createNativeQuery("""
                UPDATE sow_milestone_position_assignments
                SET status = 'ASSIGNED'
                WHERE UPPER(TRIM(status)) = 'ACTIVE'
                """).executeUpdate();

        int positionsUpdated = entityManager.createNativeQuery("""
                UPDATE sow_milestone_positions p
                INNER JOIN sow_milestones m ON m.id = p.milestone_id
                SET p.status = CASE
                    WHEN EXISTS (
                        SELECT 1
                        FROM sow_milestone_position_assignments a
                        WHERE a.milestone_position_id = p.id
                          AND UPPER(TRIM(a.status)) = 'ASSIGNED'
                    ) THEN 'ASSIGNED'
                    WHEN m.end_date IS NOT NULL AND m.end_date <= CURRENT_DATE THEN 'CLOSED'
                    ELSE 'OPEN'
                END
                """).executeUpdate();

        entityManager.createNativeQuery("""
                UPDATE application_data_migrations
                SET completed_at = CURRENT_TIMESTAMP
                WHERE migration_key = :migrationKey
                """).setParameter("migrationKey", MIGRATION_KEY).executeUpdate();

        log.info("Completed SOW position-status migration: {} assignments and {} positions updated.",
                assignmentsUpdated, positionsUpdated);
    }
}
