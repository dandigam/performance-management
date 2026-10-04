package com.rit.performance.service;

import com.rit.performance.dto.response.EmployeeLeaveSetupPageResponse;
import com.rit.performance.dto.response.EmployeeLeaveSetupResponse;
import com.rit.performance.exception.InvalidOperationException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmployeeLeaveSetupService {
    private final NamedParameterJdbcTemplate jdbc;

    // Choose one assignment before pagination; employees without assignments remain in the list.
    private static final String ROWS = """
        SELECT e.id AS employee_id, e.rit_id AS employee_number,
               TRIM(CONCAT(COALESCE(e.first_name,''), ' ', COALESCE(e.last_name,''))) AS employee_name,
               a.id AS assignment_id, p.id AS policy_id, p.policy_name,
               CASE WHEN a.id IS NOT NULL AND p.status = 'ACTIVE'
                 AND p.effective_from <= :endDate
                 AND (p.effective_to IS NULL OR p.effective_to >= :startDate)
                 AND EXISTS (SELECT 1 FROM leave_policy_rules r
                             WHERE r.leave_policy_id = p.id AND r.status = 'ACTIVE')
                 AND NOT EXISTS (
                     SELECT 1 FROM leave_policy_rules r
                     WHERE r.leave_policy_id = p.id AND r.status = 'ACTIVE'
                       AND NOT EXISTS (SELECT 1 FROM employee_leave_balances b
                           WHERE b.employee_id = e.id AND b.employee_leave_policy_id = a.id
                             AND b.leave_type_id = r.leave_type_id AND b.balance_year = :year
                             AND b.status = 'ACTIVE'))
                 THEN 'SET_UP' ELSE 'PENDING' END AS setup_status
        FROM employees e
        LEFT JOIN employee_leave_policies a ON a.id = (
            SELECT a2.id FROM employee_leave_policies a2
            WHERE a2.employee_id = e.id AND a2.status = 'ACTIVE'
              AND a2.effective_from <= :endDate
              AND (a2.effective_to IS NULL OR a2.effective_to >= :startDate)
            ORDER BY a2.effective_from DESC, a2.id DESC LIMIT 1)
        LEFT JOIN leave_policies p ON p.id = a.leave_policy_id
        """;
    private static final String FILTER = """
        WHERE (:status = 'ALL' OR s.setup_status = :status)
          AND (:search = '' OR LOWER(s.employee_name) LIKE :search ESCAPE '!'
               OR LOWER(s.employee_number) LIKE :search ESCAPE '!')
        """;

    public EmployeeLeaveSetupPageResponse list(int year, String status, String search, int page, int size) {
        if (year < 1 || year > 9999) throw new InvalidOperationException("year must be between 1 and 9999");
        if (page < 0 || size < 1 || size > 100)
            throw new InvalidOperationException("page must be non-negative and size must be between 1 and 100");
        String filter = status == null ? "ALL" : status.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("ALL", "SET_UP", "PENDING").contains(filter))
            throw new InvalidOperationException("status must be ALL, SET_UP, or PENDING");
        String term = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        if (!term.isEmpty()) term = "%" + term.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        var params = new MapSqlParameterSource()
                .addValue("startDate", java.sql.Date.valueOf(LocalDate.of(year, 1, 1)))
                .addValue("endDate", java.sql.Date.valueOf(LocalDate.of(year, 12, 31)))
                .addValue("year", year).addValue("status", filter).addValue("search", term)
                .addValue("limit", size).addValue("offset", (long) page * size);
        String source = " FROM (" + ROWS + ") s " + FILTER;
        long total = jdbc.queryForObject("SELECT COUNT(*)" + source, params, Long.class);
        var content = jdbc.query("SELECT s.*" + source
                + " ORDER BY s.employee_name, s.employee_id LIMIT :limit OFFSET :offset", params,
                (rs, row) -> new EmployeeLeaveSetupResponse(rs.getLong("employee_id"),
                        rs.getString("employee_number"), rs.getString("employee_name"),
                        rs.getObject("assignment_id", Long.class), rs.getObject("policy_id", Long.class),
                        rs.getString("policy_name"), rs.getString("setup_status")));
        return new EmployeeLeaveSetupPageResponse(content, total, (int) ((total + size - 1) / size), page, size);
    }
}
