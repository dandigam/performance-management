package com.rit.performance.repository;

import com.rit.performance.entity.*;
import jakarta.persistence.*;
import org.junit.jupiter.api.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import java.time.LocalDate;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class EmployeeSummaryQueryTest {
    static LocalContainerEntityManagerFactoryBean factory;
    EntityManager em;
    EmployeeRepository repository;
    final LocalDate today = LocalDate.of(2026, 9, 13);

    @BeforeAll static void createDatabase() {
        factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(new DriverManagerDataSource("jdbc:h2:mem:employee_summary;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", ""));
        factory.setPackagesToScan("com.rit.performance.entity");
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "create-drop"));
        factory.afterPropertiesSet();
    }
    @AfterAll static void closeDatabase() { if (factory != null) factory.destroy(); }
    @BeforeEach void begin() {
        em = factory.getObject().createEntityManager();
        em.getTransaction().begin();
        repository = new JpaRepositoryFactory(em).getRepository(EmployeeRepository.class);
    }
    @AfterEach void rollback() { em.getTransaction().rollback(); em.close(); }

    @Test void filtersBeforeCountingAndPaging() {
        for (int i = 0; i < 53; i++) employee(String.format("Employee%02d", i), "ACTIVE");
        employee("Inactive", "INACTIVE");
        var page = repository.findSummaries(null, null, null, null, null, "ACTIVE", today,
                PageRequest.of(0, 20, Sort.by("firstName")));
        assertThat(page.getTotalElements()).isEqualTo(53);
        assertThat(page.getTotalPages()).isEqualTo(3);
        assertThat(page.getContent()).hasSize(20);
        assertThat(page.getContent().get(0).getFirstName()).isEqualTo("Employee00");
        var last = repository.findSummaries(null, null, null, null, null, "ACTIVE", today,
                PageRequest.of(2, 20, Sort.by("firstName")));
        assertThat(last.getContent()).hasSize(13);
        assertThat(last.getTotalElements()).isEqualTo(53);
    }

    @Test void combinesFiltersSearchesRelatedNamesAndIgnoresEndedOrFutureAssignments() {
        var type = LookupType.builder().code("TEST").name("Test").build(); em.persist(type);
        var department = LookupValue.builder().lookupType(type).code("ENG").name("Engineering").build(); em.persist(department);
        var designation = LookupValue.builder().lookupType(type).code("LEAD").name("Technical Lead").build(); em.persist(designation);
        var sow = Sow.builder().sowName("CBMS Support").sowType("TEST").engagementType("TEST")
                .status(department).businessUnit(department).build(); em.persist(sow);
        var charan = employee("Charan", "ACTIVE"); charan.setDesignationId(designation.getId());
        charan.setRitId("RIT03"); charan.setWorkMode("ONSITE");
        assignment(charan, sow, "ASSIGNED", today.minusDays(10), null);
        assignment(charan, sow, "ASSIGNED", today.minusDays(5), null);
        var ended = employee("Ended", "ACTIVE");
        assignment(ended, sow, "COMPLETED", today.minusDays(30), today.minusDays(1));
        var future = employee("Future", "ACTIVE");
        assignment(future, sow, "ASSIGNED", today.plusDays(1), null);
        var expired = employee("Expired", "ACTIVE");
        assignment(expired, sow, "ASSIGNED", today.minusDays(30), today.minusDays(1));
        var pageable = PageRequest.of(0, 1, Sort.by("firstName"));
        for (String search : new String[]{"%charan%", "%rit03%", "%charan@example.com%", "%technical%", "%engineering%", "%cbms%"}) {
            var result = repository.findSummaries(search, department.getId(), sow.getId(), "ASSIGNED", "ONSITE", "ACTIVE", today, pageable);
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent()).containsExactly(charan);
        }
        var unassigned = repository.findSummaries(null, null, null, "UNASSIGNED", null, null, today, PageRequest.of(0, 20));
        assertThat(unassigned.getContent()).containsExactlyInAnyOrder(ended, future, expired);
        assertThat(repository.findSummaries(null, department.getId(), null, "UNASSIGNED", null, null, today, pageable).getTotalElements()).isZero();
        assertThat(repository.findSummaries("%missing%", null, null, null, null, null, today, pageable).getTotalElements()).isZero();
    }

    private Employee employee(String name, String status) {
        var employee = new Employee(); employee.setFirstName(name); employee.setEmail(name.toLowerCase()+"@example.com");
        employee.setStatus(status); employee.setJoiningDate(today.minusYears(1)); em.persist(employee); return employee;
    }
    private void assignment(Employee employee, Sow sow, String status, LocalDate start, LocalDate end) {
        var assignment = new EmployeeAssignment(); assignment.setEmployeeId(employee.getId()); assignment.setSowId(sow.getId());
        assignment.setStatus(status); assignment.setEffectiveFrom(start); assignment.setEffectiveTo(end); em.persist(assignment);
    }
}
