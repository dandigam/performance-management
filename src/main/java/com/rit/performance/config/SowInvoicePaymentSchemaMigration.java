package com.rit.performance.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Migrates legacy invoice receipt fields after Hibernate has created the new schema. */
@Component
@Order(310)
@RequiredArgsConstructor
public class SowInvoicePaymentSchemaMigration implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        if (!tableExists("sow_invoices") || !tableExists("sow_invoice_payments")) return;

        if (columnExists("sow_invoices", "actual_invoice_date")) {
            jdbcTemplate.update("update sow_invoices set invoice_raised_date = actual_invoice_date "
                    + "where invoice_raised_date is null");
        }
        if (columnExists("sow_invoices", "invoice_date")) {
            jdbcTemplate.update("update sow_invoices set invoice_raised_date = invoice_date "
                    + "where invoice_raised_date is null");
        }
        if (columnExists("sow_invoices", "invoice_amount")) {
            jdbcTemplate.update("update sow_invoices set invoice_raised_amount = invoice_amount "
                    + "where invoice_raised_amount is null");
        }

        if (columnExists("sow_invoices", "planned_invoice_date")) {
            jdbcTemplate.update("update sow_invoices set milestone_invoice_date = "
                    + "coalesce(milestone_invoice_date, planned_invoice_date)");
        }
        if (columnExists("sow_invoices", "planned_invoice_amount")) {
            jdbcTemplate.update("update sow_invoices set milestone_invoice_amount = "
                    + "coalesce(milestone_invoice_amount, planned_invoice_amount)");
        }

        jdbcTemplate.update("""
                update sow_invoices invoice
                join sow_milestones milestone on milestone.id = invoice.milestone_id
                set invoice.milestone_invoice_date = coalesce(
                        invoice.milestone_invoice_date, milestone.invoice_date),
                    invoice.milestone_invoice_amount = coalesce(
                        invoice.milestone_invoice_amount, milestone.amount)
                """);

        jdbcTemplate.update("""
                update sow_invoices
                set invoice_number = concat('INV-', lpad(id, 6, '0'))
                where invoice_number is null or trim(invoice_number) = ''
                """);

        if (!indexExists("sow_invoices", "uk_sow_invoice_number")) {
            jdbcTemplate.execute("create unique index uk_sow_invoice_number "
                    + "on sow_invoices (invoice_number)");
        }

        if (columnExists("sow_invoices", "payment_received_date")
                && columnExists("sow_invoices", "received_amount")) {
            jdbcTemplate.update("""
                    insert into sow_invoice_payments
                        (invoice_id, payment_date, received_amount, created_on, updated_on)
                    select invoice.id, invoice.payment_received_date, invoice.received_amount,
                           now(6), now(6)
                    from sow_invoices invoice
                    where invoice.payment_received_date is not null
                      and invoice.received_amount is not null
                      and invoice.received_amount > 0
                      and not exists (
                          select 1 from sow_invoice_payments payment
                          where payment.invoice_id = invoice.id
                            and payment.payment_date = invoice.payment_received_date
                            and payment.received_amount = invoice.received_amount
                      )
                    """);
        }

        jdbcTemplate.update("update sow_invoice_payments "
                + "set payment_method = 'PAYMENT_RECEIVED'");

        dropIndexIfExists("sow_invoices", "idx_sow_invoices_payment_status");
        dropColumnIfExists("sow_invoices", "payment_received_date");
        dropColumnIfExists("sow_invoices", "received_amount");
        dropColumnIfExists("sow_invoices", "payment_status");
        dropColumnIfExists("sow_invoices", "actual_invoice_date");
        dropColumnIfExists("sow_invoices", "invoice_date");
        dropColumnIfExists("sow_invoices", "invoice_amount");
        dropColumnIfExists("sow_invoices", "planned_invoice_date");
        dropColumnIfExists("sow_invoices", "planned_invoice_amount");
    }

    private void dropColumnIfExists(String table, String column) {
        if (columnExists(table, column)) {
            jdbcTemplate.execute("alter table `" + table + "` drop column `" + column + "`");
        }
    }

    private void dropIndexIfExists(String table, String index) {
        if (indexExists(table, index)) {
            jdbcTemplate.execute("alter table `" + table + "` drop index `" + index + "`");
        }
    }

    private boolean tableExists(String table) {
        Integer count = jdbcTemplate.queryForObject("""
                select count(*) from information_schema.tables
                where table_schema = database() and table_name = ?
                """, Integer.class, table);
        return count != null && count > 0;
    }

    private boolean columnExists(String table, String column) {
        Integer count = jdbcTemplate.queryForObject("""
                select count(*) from information_schema.columns
                where table_schema = database() and table_name = ? and column_name = ?
                """, Integer.class, table, column);
        return count != null && count > 0;
    }

    private boolean indexExists(String table, String index) {
        Integer count = jdbcTemplate.queryForObject("""
                select count(*) from information_schema.statistics
                where table_schema = database() and table_name = ? and index_name = ?
                """, Integer.class, table, index);
        return count != null && count > 0;
    }
}
