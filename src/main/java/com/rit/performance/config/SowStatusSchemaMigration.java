package com.rit.performance.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Backfills the lookup-backed SOW status before enforcing referential integrity. */
@Component
@Order(1)
@RequiredArgsConstructor
public class SowStatusSchemaMigration implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!columnExists("status_id")) {
            jdbcTemplate.execute("alter table sows add column status_id bigint null after end_date");
        }

        if (columnExists("status")) {
            jdbcTemplate.update("""
                    update sows sow_row
                    join lookup_types status_type
                      on upper(status_type.code) = 'SOW_STATUS'
                    join lookup_values status_value
                      on status_value.lookup_type_id = status_type.id
                     and upper(status_value.code) = coalesce(
                         nullif(upper(trim(sow_row.status)), ''), 'DRAFT')
                    set sow_row.status_id = status_value.id
                    """);
        }

        jdbcTemplate.update("""
                update sows sow_row
                join lookup_types status_type
                  on upper(status_type.code) = 'SOW_STATUS'
                join lookup_values draft_status
                  on draft_status.lookup_type_id = status_type.id
                 and upper(draft_status.code) = 'DRAFT'
                left join lookup_values current_status
                  on current_status.id = sow_row.status_id
                 and current_status.lookup_type_id = status_type.id
                set sow_row.status_id = draft_status.id
                where current_status.id is null
                """);

        if (invalidStatusCount() > 0) {
            throw new IllegalStateException("Unable to backfill every SOW status_id");
        }

        if (!indexExists("idx_sow_status_id")) {
            jdbcTemplate.execute("create index idx_sow_status_id on sows (status_id)");
        }
        if (!foreignKeyExists("fk_sow_status")) {
            jdbcTemplate.execute("alter table sows add constraint fk_sow_status "
                    + "foreign key (status_id) references lookup_values (id)");
        }
        jdbcTemplate.execute("alter table sows modify status_id bigint not null");
    }

    private int invalidStatusCount() {
        Integer count = jdbcTemplate.queryForObject("""
                select count(*)
                from sows sow_row
                left join lookup_values status_value on status_value.id = sow_row.status_id
                left join lookup_types status_type
                  on status_type.id = status_value.lookup_type_id
                 and upper(status_type.code) = 'SOW_STATUS'
                where status_type.id is null
                """, Integer.class);
        return count == null ? 0 : count;
    }

    private boolean columnExists(String columnName) {
        Integer count = jdbcTemplate.queryForObject("""
                select count(*) from information_schema.columns
                where table_schema = database() and table_name = 'sows' and column_name = ?
                """, Integer.class, columnName);
        return count != null && count > 0;
    }

    private boolean indexExists(String indexName) {
        Integer count = jdbcTemplate.queryForObject("""
                select count(*) from information_schema.statistics
                where table_schema = database() and table_name = 'sows' and index_name = ?
                """, Integer.class, indexName);
        return count != null && count > 0;
    }

    private boolean foreignKeyExists(String constraintName) {
        Integer count = jdbcTemplate.queryForObject("""
                select count(*) from information_schema.table_constraints
                where table_schema = database() and table_name = 'sows'
                  and constraint_type = 'FOREIGN KEY' and constraint_name = ?
                """, Integer.class, constraintName);
        return count != null && count > 0;
    }
}
