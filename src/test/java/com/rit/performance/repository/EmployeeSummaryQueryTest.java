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
import java.util.List;
import java.util.Map;
import tools.jackson.databind.node.StringNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.LongNode;
import com.rit.performance.dto.report.ReportFilterRequest;
import com.rit.performance.dto.report.ReportQueryRequest;
import com.rit.performance.dto.report.ReportSortRequest;
import com.rit.performance.service.EmployeeReportQueryService;
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

    @Test void summariesOnlyIncludeActiveAndInactiveBeforePaging() {
        var active = employee("Active", "ACTIVE");
        var inactive = employee("Inactive", "INACTIVE");
        employee("Pending", "PENDING");
        employee("Onboarding", "ONBOARDING");
        employee("Invited", "INVITED");
        employee("Submitted", "SUBMITTED");
        var first = repository.findSummaries(null, null, null, null, null, null, null, today,
                PageRequest.of(0, 1, Sort.by("firstName")));
        assertThat(first.getContent()).containsExactly(active);
        assertThat(first.getTotalElements()).isEqualTo(2);
        assertThat(first.getTotalPages()).isEqualTo(2);
        var second = repository.findSummaries(null, null, null, null, null, null, null, today,
                PageRequest.of(1, 1, Sort.by("firstName")));
        assertThat(second.getContent()).containsExactly(inactive);
        assertThat(second.getTotalElements()).isEqualTo(2);
        var excluded = repository.findSummaries(null, null, null, null, null, null, "ONBOARDING", today,
                PageRequest.of(0, 20));
        assertThat(excluded.getTotalElements()).isZero();
        assertThat(excluded.getContent()).isEmpty();
    }

    @Test void filtersBeforeCountingAndPaging() {
        for (int i = 0; i < 53; i++) employee(String.format("Employee%02d", i), "ACTIVE");
        employee("Inactive", "INACTIVE");
        var page = repository.findSummaries(null, null, null, null, null, null, "ACTIVE", today,
                PageRequest.of(0, 20, Sort.by("firstName")));
        assertThat(page.getTotalElements()).isEqualTo(53);
        assertThat(page.getTotalPages()).isEqualTo(3);
        assertThat(page.getContent()).hasSize(20);
        assertThat(page.getContent().get(0).getFirstName()).isEqualTo("Employee00");
        var last = repository.findSummaries(null, null, null, null, null, null, "ACTIVE", today,
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
        charan.setRitId("RIT03"); charan.setWorkMode("ONSITE"); charan.setWorkLocation("HYBRID");
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
            var result = repository.findSummaries(search, department.getId(), sow.getId(), "ASSIGNED",
                    "ONSITE", "HYBRID", "ACTIVE", today, pageable);
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent()).containsExactly(charan);
        }
        var unassigned = repository.findSummaries(null, null, null, "UNASSIGNED", null, null, null,
                today, PageRequest.of(0, 20));
        assertThat(unassigned.getContent()).containsExactlyInAnyOrder(ended, future, expired);
        assertThat(repository.findSummaries(null, department.getId(), null, "UNASSIGNED", null, null,
                null, today, pageable).getTotalElements()).isZero();
        assertThat(repository.findSummaries("%missing%", null, null, null, null, null, null,
                today, pageable).getTotalElements()).isZero();
    }

    @Test void workforceReportCombinesFiltersBeforePaging() {
        var type = LookupType.builder().code("REPORT_TEST").name("Report Test").build(); em.persist(type);
        var department = LookupValue.builder().lookupType(type).code("ENG").name("Engineering").build(); em.persist(department);
        var designation = LookupValue.builder().lookupType(type).code("TL").name("Technical Lead").build(); em.persist(designation);
        var sow = Sow.builder().sowName("Engineering Delivery").sowType("TEST").engagementType("TEST")
                .status(department).businessUnit(department).build(); em.persist(sow);

        var matching = employee("Charan", "ACTIVE");
        matching.setRitId("RIT03");
        matching.setDesignationId(designation.getId());
        matching.setEmploymentType("FULL_TIME");
        matching.setWorkMode("OFFSHORE");
        matching.setWorkLocation("REMOTE");
        assignment(matching, sow, "ASSIGNED", today.minusDays(1), null);

        var excluded = employee("Contractor", "ACTIVE");
        excluded.setDesignationId(designation.getId());
        excluded.setEmploymentType("CONTRACT");
        excluded.setWorkMode("ONSITE");
        excluded.setWorkLocation("OFFICE");

        var page = repository.findWorkforceReport(
                "%engineering%", department.getId(), designation.getId(), "FULL_TIME",
                "OFFSHORE", "REMOTE", "ACTIVE", today,
                PageRequest.of(0, 1, Sort.by("firstName")));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getTotalPages()).isEqualTo(1);
        assertThat(page.getContent()).containsExactly(matching);
    }

    @Test void metadataDrivenWorkModeFilterReturnsOnsiteEmployees() {
        var onsite = employee("Onsite", "ACTIVE");
        onsite.setWorkMode("ONSITE");
        var offshore = employee("Offshore", "ACTIVE");
        offshore.setWorkMode("OFFSHORE");

        var repositories = new JpaRepositoryFactory(em);
        var service = new EmployeeReportQueryService(
                repository,
                repositories.getRepository(EmployeeAssignmentRepository.class),
                repositories.getRepository(SowRepository.class),
                repositories.getRepository(LookupValueRepository.class));
        var request = new ReportQueryRequest(
                List.of("employeeName", "workMode", "assignmentStatus"),
                List.of(new ReportFilterRequest("workMode", "EQUALS", StringNode.valueOf("ONSITE"))),
                List.of(new ReportSortRequest("employeeName", "ASC")), 0, 25);

        var result = service.query(request);

        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0))
                .containsEntry("employeeName", "Onsite")
                .containsEntry("workMode", "ONSITE");
    }

    @Test void metadataDefinitionOperatorsExecuteForAllFilterKinds() {
        var type = LookupType.builder().code("FILTER_TEST").name("Filter Test").build(); em.persist(type);
        var department = LookupValue.builder().lookupType(type).code("ENG").name("Engineering").build(); em.persist(department);
        var designation = LookupValue.builder().lookupType(type).code("LEAD").name("Technical Lead").build(); em.persist(designation);
        var sow = Sow.builder().sowName("Delivery").sowType("TEST").engagementType("TEST")
                .status(department).businessUnit(department).build(); em.persist(sow);
        var employee = employee("Filterable", "ACTIVE");
        employee.setLastName("Employee");
        employee.setRitId("RIT99");
        employee.setDesignationId(designation.getId());
        employee.setEmploymentType("FULL_TIME");
        employee.setWorkMode("ONSITE");
        employee.setWorkLocation("REMOTE");
        employee.setJoiningDate(today.minusDays(10));
        assignment(employee, sow, "ASSIGNED", today.minusDays(5), null);

        var repositories = new JpaRepositoryFactory(em);
        var service = new EmployeeReportQueryService(repository,
                repositories.getRepository(EmployeeAssignmentRepository.class),
                repositories.getRepository(SowRepository.class),
                repositories.getRepository(LookupValueRepository.class));

        assertMatch(service, "employeeName", "CONTAINS", StringNode.valueOf("filter"));
        assertMatch(service, "employeeNumber", "EQUALS", StringNode.valueOf("RIT99"));
        assertMatch(service, "designationName", "CONTAINS", StringNode.valueOf("technical"));
        assertMatch(service, "designationId", "EQUALS", LongNode.valueOf(designation.getId()));
        assertMatch(service, "designationId", "IN", array(designation.getId()));
        assertMatch(service, "departmentId", "EQUALS", LongNode.valueOf(department.getId()));
        assertMatch(service, "departmentId", "IN", array(department.getId()));
        assertMatch(service, "departmentId", "EQUALS", StringNode.valueOf(department.getId().toString()));
        assertMatch(service, "designationId", "EQUALS", StringNode.valueOf(designation.getId().toString()));
        assertThat(service.query(new ReportQueryRequest(
                List.of("employeeName", "departmentName", "designationName"),
                List.of(
                        new ReportFilterRequest("designationId", "EQUALS",
                                StringNode.valueOf(designation.getId().toString())),
                        new ReportFilterRequest("departmentId", "EQUALS",
                                StringNode.valueOf(department.getId().toString()))),
                List.of(), 0, 25)).totalElements()).isEqualTo(1);
        assertMatch(service, "employmentType", "IN", array("FULL_TIME"));
        assertMatch(service, "workMode", "EQUALS", StringNode.valueOf("ONSITE"));
        assertMatch(service, "workLocation", "EQUALS", StringNode.valueOf("REMOTE"));
        assertMatch(service, "status", "EQUALS", StringNode.valueOf("ACTIVE"));
        assertMatch(service, "assignmentStatus", "EQUALS", StringNode.valueOf("ASSIGNED"));
        assertMatch(service, "joiningDate", "BEFORE", StringNode.valueOf(today.toString()));
        assertMatch(service, "joiningDate", "AFTER", StringNode.valueOf(today.minusDays(20).toString()));

        assertNoMatch(service, "workMode", "EQUALS", StringNode.valueOf("DOES_NOT_EXIST"));
        assertNoMatch(service, "assignmentStatus", "EQUALS", StringNode.valueOf("UNASSIGNED"));
        assertNoMatch(service, "joiningDate", "IS_EMPTY", null);

        var assignedSummary = query(service, "assignmentStatus", "EQUALS", StringNode.valueOf("ASSIGNED"));
        assertThat(assignedSummary.summary())
                .containsEntry("totalEmployees", 1L)
                .containsEntry("assignedEmployees", 1L)
                .containsEntry("unassignedEmployees", 0L);
        var unassignedSummary = query(service, "assignmentStatus", "EQUALS", StringNode.valueOf("UNASSIGNED"));
        assertThat(unassignedSummary.summary())
                .containsEntry("totalEmployees", 0L)
                .containsEntry("assignedEmployees", 0L)
                .containsEntry("unassignedEmployees", 0L);
    }

    private void assertMatch(EmployeeReportQueryService service, String field, String operator,
            tools.jackson.databind.JsonNode value) {
        assertThat(query(service, field, operator, value).totalElements()).isEqualTo(1);
    }

    private void assertNoMatch(EmployeeReportQueryService service, String field, String operator,
            tools.jackson.databind.JsonNode value) {
        var result = query(service, field, operator, value);
        assertThat(result.totalElements()).isZero();
        assertThat(result.content()).isEmpty();
    }

    private com.rit.performance.dto.report.GenericReportResponse query(
            EmployeeReportQueryService service, String field, String operator,
            tools.jackson.databind.JsonNode value) {
        return service.query(new ReportQueryRequest(
                List.of("employeeName"), List.of(new ReportFilterRequest(field, operator, value)),
                List.of(new ReportSortRequest("employeeName", "ASC")), 0, 25));
    }

    private static ArrayNode array(long value) {
        return JsonNodeFactory.instance.arrayNode().add(value);
    }

    private static ArrayNode array(String value) {
        return JsonNodeFactory.instance.arrayNode().add(value);
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
