package com.rit.performance.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/** Allows an employee to have multiple experience records. */
@Component
@RequiredArgsConstructor
public class EmployeeExperienceSchemaMigration implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        List<String> uniqueIndexes = jdbcTemplate.queryForList("""
                select distinct index_name
                from information_schema.statistics
                where table_schema = database()
                  and table_name = 'employee_experiences'
                  and column_name = 'employee_id'
                  and non_unique = 0
                  and index_name <> 'PRIMARY'
                """, String.class);

        if (!uniqueIndexes.isEmpty()) {
            Integer replacementIndexCount = jdbcTemplate.queryForObject("""
                    select count(*)
                    from information_schema.statistics
                    where table_schema = database()
                      and table_name = 'employee_experiences'
                      and index_name = 'idx_employee_experiences_employee'
                    """, Integer.class);
            if (replacementIndexCount == null || replacementIndexCount == 0) {
                jdbcTemplate.execute("create index idx_employee_experiences_employee "
                        + "on employee_experiences (employee_id)");
            }
        }

        for (String indexName : uniqueIndexes) {
            jdbcTemplate.execute("alter table employee_experiences drop index `"
                    + indexName.replace("`", "``") + "`");
        }
    }
}
