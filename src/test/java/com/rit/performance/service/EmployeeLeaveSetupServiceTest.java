package com.rit.performance.service;

import com.rit.performance.exception.InvalidOperationException;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import static org.assertj.core.api.Assertions.*;

class EmployeeLeaveSetupServiceTest {
    private JdbcTemplate jdbc;
    private EmployeeLeaveSetupService service;
    @BeforeEach void setup() {
        var ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:" + java.util.UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        jdbc = new JdbcTemplate(ds);
        service = new EmployeeLeaveSetupService(new NamedParameterJdbcTemplate(ds));
        jdbc.execute("CREATE TABLE employees(id BIGINT PRIMARY KEY, rit_id VARCHAR(50), first_name VARCHAR(100), last_name VARCHAR(100))");
        jdbc.execute("CREATE TABLE leave_policies(id BIGINT PRIMARY KEY, policy_name VARCHAR(100), status VARCHAR(20), effective_from DATE, effective_to DATE)");
        jdbc.execute("CREATE TABLE employee_leave_policies(id BIGINT PRIMARY KEY, employee_id BIGINT, leave_policy_id BIGINT, status VARCHAR(20), effective_from DATE, effective_to DATE)");
        jdbc.execute("CREATE TABLE leave_policy_rules(id BIGINT PRIMARY KEY, leave_policy_id BIGINT, leave_type_id BIGINT, status VARCHAR(20))");
        jdbc.execute("CREATE TABLE employee_leave_balances(employee_id BIGINT, employee_leave_policy_id BIGINT, leave_type_id BIGINT, balance_year INT, status VARCHAR(20))");
        jdbc.execute("INSERT INTO employees VALUES (1,'RIT1','Alice','Test'),(2,'RIT2','Bob',NULL),(3,'RIT3','Carol','Test')");
        jdbc.execute("INSERT INTO leave_policies VALUES (1,'Policy','ACTIVE','2026-01-01',NULL)");
        jdbc.execute("INSERT INTO employee_leave_policies VALUES (1,1,1,'ACTIVE','2026-01-01',NULL),(2,2,1,'ACTIVE','2026-01-01',NULL)");
        jdbc.execute("INSERT INTO leave_policy_rules VALUES (1,1,1,'ACTIVE'),(2,1,2,'ACTIVE')");
        jdbc.execute("INSERT INTO employee_leave_balances VALUES (1,1,1,2026,'ACTIVE'),(1,1,2,2026,'ACTIVE'),(2,2,1,2026,'ACTIVE')");
    }
    @Test void includesUnassignedAndPartiallyInitializedEmployees() {
        var result = service.list(2026,"ALL","",0,20);
        assertThat(result.totalElements()).isEqualTo(3);
        assertThat(result.content()).extracting(r -> r.setupStatus()).containsExactly("SET_UP","PENDING","PENDING");
        assertThat(result.content().get(2).employeeLeavePolicyId()).isNull();
        assertThat(result.content().get(1).employeeLeavePolicyId()).isEqualTo(2L);
    }
    @Test void filtersBeforePaginationAndSearchesNamesAndNumbers() {
        var result = service.list(2026,"PENDING","",1,1);
        assertThat(result.totalElements()).isEqualTo(2);
        assertThat(result.totalPages()).isEqualTo(2);
        assertThat(result.content().get(0).employeeId()).isEqualTo(3L);
        assertThat(service.list(2026,"ALL","alice test",0,20).totalElements()).isEqualTo(1);
        assertThat(service.list(2026,"ALL","RIT2",0,20).content().get(0).employeeId()).isEqualTo(2L);
        assertThat(service.list(2026,"ALL","%",0,20).totalElements()).isZero();
        assertThat(service.list(2026,"ALL","",9,20).content()).isEmpty();
    }
    @Test void inactiveAndWrongYearBalancesDoNotCompleteSetup() {
        jdbc.execute("UPDATE employee_leave_balances SET status='INACTIVE' WHERE employee_id=1 AND leave_type_id=2");
        assertThat(service.list(2026,"SET_UP","",0,20).totalElements()).isZero();
        assertThat(service.list(2027,"SET_UP","",0,20).totalElements()).isZero();
    }
    @Test void selectsLatestOverlappingAssignmentWithoutDuplicatingEmployee() {
        jdbc.execute("UPDATE employee_leave_policies SET effective_to='2026-06-30' WHERE id=1");
        jdbc.execute("INSERT INTO employee_leave_policies VALUES (3,1,1,'ACTIVE','2026-07-01',NULL),(4,1,1,'ACTIVE','2027-01-01',NULL)");
        var result=service.list(2026,"ALL","RIT1",0,20);
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.content().get(0).employeeLeavePolicyId()).isEqualTo(3L);
        assertThat(result.content().get(0).setupStatus()).isEqualTo("PENDING");
    }
    @Test void inactiveAssignmentsAndEmptyPoliciesArePending() {
        jdbc.execute("UPDATE employee_leave_policies SET status='INACTIVE' WHERE id=1");
        assertThat(service.list(2026,"ALL","RIT1",0,20).content().get(0).employeeLeavePolicyId()).isNull();
        jdbc.execute("DELETE FROM leave_policy_rules");
        assertThat(service.list(2026,"SET_UP","",0,20).totalElements()).isZero();
    }
    @Test void validatesInputs() {
        assertThatThrownBy(() -> service.list(2026,"BAD","",0,20)).isInstanceOf(InvalidOperationException.class);
        assertThatThrownBy(() -> service.list(2026,"ALL","",-1,20)).isInstanceOf(InvalidOperationException.class);
        assertThatThrownBy(() -> service.list(2026,"ALL","",0,101)).isInstanceOf(InvalidOperationException.class);
        assertThatThrownBy(() -> service.list(0,"ALL","",0,20)).isInstanceOf(InvalidOperationException.class);
    }
}
