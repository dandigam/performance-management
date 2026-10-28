package com.rit.performance.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Applies outstanding production schema adjustments after Hibernate schema update. */
@Component
@Profile("prod")
@RequiredArgsConstructor
@Slf4j
public class ProductionReferenceDataInitializer implements ApplicationRunner {
    private final JdbcTemplate jdbc;

    @Override
    public void run(ApplicationArguments args) {
        var columns = jdbc.queryForList("""
                SELECT IS_NULLABLE, DATA_TYPE
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'employees'
                  AND COLUMN_NAME = 'joining_date'
                """);
        if (columns.size() != 1 || !"date".equalsIgnoreCase(String.valueOf(columns.get(0).get("DATA_TYPE")))) {
            throw new IllegalStateException("Expected employees.joining_date DATE before enabling employee import");
        }
        if ("NO".equalsIgnoreCase(String.valueOf(columns.get(0).get("IS_NULLABLE")))) {
            // MySQL DDL implicitly commits: keep this outside a transaction and preserve all stored dates.
            jdbc.execute("ALTER TABLE employees MODIFY COLUMN joining_date DATE NULL");
            log.info("Made employees.joining_date optional for performance-only employee imports.");
        } else {
            log.info("employees.joining_date already allows NULL; no production schema adjustment needed.");
        }
    }
}
