package com.rit.performance.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/** Aligns the derived headcount table with skill-based grouping. */
@Component
@Order(300)
@RequiredArgsConstructor
public class SowResourceRequirementSchemaMigration implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        List<String> requiredColumns = List.of(
                "sow_id", "position_id", "skill_id", "seniority", "location");
        if (!indexColumns("uk_resource_requirement").equals(requiredColumns)) {
            // Derived rows are rebuilt from milestone positions after the next SOW mutation.
            jdbcTemplate.update("delete from sow_resource_requirement");
            ensureIndex("idx_resource_requirement_sow", "sow_id");
            ensureIndex("idx_resource_requirement_position", "position_id");
            ensureIndex("idx_resource_requirement_skill", "skill_id");
            if (indexExists("uk_resource_requirement")) {
                jdbcTemplate.execute("alter table sow_resource_requirement "
                        + "drop index uk_resource_requirement");
            }
            if (indexExists("uk_resource_requirement_skill")) {
                jdbcTemplate.execute("alter table sow_resource_requirement "
                        + "drop index uk_resource_requirement_skill");
            }
            jdbcTemplate.execute("alter table sow_resource_requirement add constraint "
                    + "uk_resource_requirement unique "
                    + "(sow_id, position_id, skill_id, seniority, location)");
        }

        dropForeignKeys("position_id");
        dropForeignKeys("skill_id");

        for (String column : List.of(
                "assigned_hc", "total_hours", "created_at", "updated_at")) {
            if (columnExists(column)) {
                jdbcTemplate.execute("alter table sow_resource_requirement drop column `"
                        + column + "`");
            }
        }
    }

    private List<String> indexColumns(String indexName) {
        return jdbcTemplate.queryForList("""
                select column_name
                from information_schema.statistics
                where table_schema = database()
                  and table_name = 'sow_resource_requirement'
                  and index_name = ?
                order by seq_in_index
                """, String.class, indexName);
    }

    private void dropForeignKeys(String columnName) {
        List<String> foreignKeys = jdbcTemplate.queryForList("""
                select constraint_name
                from information_schema.key_column_usage
                where table_schema = database()
                  and table_name = 'sow_resource_requirement'
                  and column_name = ?
                  and referenced_table_name is not null
                """, String.class, columnName);
        for (String foreignKey : foreignKeys) {
            jdbcTemplate.execute("alter table sow_resource_requirement drop foreign key `"
                    + foreignKey.replace("`", "``") + "`");
        }
    }

    private void ensureIndex(String indexName, String columnName) {
        if (!indexExists(indexName)) {
            jdbcTemplate.execute("create index " + indexName
                    + " on sow_resource_requirement (" + columnName + ")");
        }
    }

    private boolean indexExists(String indexName) {
        Integer count = jdbcTemplate.queryForObject("""
                select count(*)
                from information_schema.statistics
                where table_schema = database()
                  and table_name = 'sow_resource_requirement'
                  and index_name = ?
                """, Integer.class, indexName);
        return count != null && count > 0;
    }

    private boolean columnExists(String columnName) {
        Integer count = jdbcTemplate.queryForObject("""
                select count(*)
                from information_schema.columns
                where table_schema = database()
                  and table_name = 'sow_resource_requirement'
                  and column_name = ?
                """, Integer.class, columnName);
        return count != null && count > 0;
    }
}
