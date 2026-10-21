package com.rit.performance.repository;

import com.rit.performance.entity.Employee;
import com.rit.performance.entity.EmployeeCompensation;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class EmployeeCompensationPersistenceTest extends EmployeeSummaryQueryTest {
    @Test void storesAnnualSalaryWithoutHourlyRateAndHourlyRateWithoutAnnualSalary() {
        var employee = new Employee();
        employee.setFirstName("Pay Test");
        employee.setEmail("pay-test@example.com");
        employee.setJoiningDate(LocalDate.of(2026, 1, 1));
        em.persist(employee);

        var salary = EmployeeCompensation.builder().employee(employee).payType("W2_SALARY")
                .annualSalary(new BigDecimal("45000.00")).currency("USD")
                .effectiveDate(LocalDate.of(2026, 1, 1)).current(false).build();
        var hourly = EmployeeCompensation.builder().employee(employee).payType("W2_HOURLY")
                .hourlyRate(new BigDecimal("25.00")).currency("USD")
                .effectiveDate(LocalDate.of(2026, 9, 1)).current(true).build();
        em.persist(salary);
        em.persist(hourly);
        em.flush();
        em.clear();

        assertThat(em.find(EmployeeCompensation.class, salary.getId()).getAnnualSalary())
                .isEqualByComparingTo("45000.00");
        assertThat(em.find(EmployeeCompensation.class, salary.getId()).getHourlyRate()).isNull();
        assertThat(em.find(EmployeeCompensation.class, hourly.getId()).getHourlyRate())
                .isEqualByComparingTo("25.00");
        assertThat(em.find(EmployeeCompensation.class, hourly.getId()).getAnnualSalary()).isNull();
    }
}
