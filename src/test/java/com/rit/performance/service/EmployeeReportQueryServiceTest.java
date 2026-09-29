package com.rit.performance.service;

import com.rit.performance.dto.report.ReportQueryRequest;
import com.rit.performance.dto.report.ReportSortRequest;
import com.rit.performance.entity.Employee;
import com.rit.performance.exception.InvalidOperationException;
import com.rit.performance.repository.EmployeeAssignmentRepository;
import com.rit.performance.repository.EmployeeRepository;
import com.rit.performance.repository.LookupValueRepository;
import com.rit.performance.repository.SowRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EmployeeReportQueryServiceTest {

    private final EmployeeRepository employeeRepository = mock(EmployeeRepository.class);
    private final EmployeeAssignmentRepository assignmentRepository = mock(EmployeeAssignmentRepository.class);
    private final SowRepository sowRepository = mock(SowRepository.class);
    private final LookupValueRepository lookupValueRepository = mock(LookupValueRepository.class);
    private final EmployeeReportQueryService service = new EmployeeReportQueryService(
            employeeRepository, assignmentRepository, sowRepository, lookupValueRepository);

    @Test
    void definitionPublishesLookupAndEnumMetadata() {
        var definition = service.definition();

        assertThat(definition.reportCode()).isEqualTo("EMPLOYEE_WORKFORCE");
        assertThat(definition.columns()).anySatisfy(column -> {
            assertThat(column.key()).isEqualTo("departmentId");
            assertThat(column.lookupCode()).isEqualTo("DEPARTMENT");
            assertThat(column.operators()).containsExactly("EQUALS", "IN", "IS_EMPTY");
        });
        assertThat(definition.columns()).anySatisfy(column -> {
            assertThat(column.key()).isEqualTo("assignmentStatus");
            assertThat(column.options()).extracting("value")
                    .containsExactly("ASSIGNED", "UNASSIGNED");
        });
    }

    @Test
    void queryReturnsOnlyRequestedApprovedColumns() {
        Employee employee = new Employee();
        employee.setId(2L);
        employee.setRitId("RIT03");
        employee.setFirstName("Charan");
        employee.setLastName("Kovvuru");
        employee.setJoiningDate(LocalDate.of(2023, 7, 12));
        when(employeeRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class),
                any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(employee)));
        when(employeeRepository.count(any(org.springframework.data.jpa.domain.Specification.class)))
                .thenReturn(1L, 0L);
        when(assignmentRepository.findCurrentForEmployees(any(), any())).thenReturn(List.of());
        when(sowRepository.findAllById(any())).thenReturn(List.of());
        when(lookupValueRepository.findAllById(any())).thenReturn(List.of());

        var response = service.query(new ReportQueryRequest(
                List.of("employeeNumber", "employeeName", "assignmentStatus"),
                List.of(), List.of(new ReportSortRequest("employeeName", "ASC")), 0, 25));

        assertThat(response.content()).singleElement().satisfies(row -> {
            assertThat(row).containsOnlyKeys("employeeNumber", "employeeName", "assignmentStatus");
            assertThat(row.get("assignmentStatus")).isEqualTo("UNASSIGNED");
        });
        assertThat(response.summary()).containsEntry("totalEmployees", 1L)
                .containsEntry("assignedEmployees", 0L)
                .containsEntry("unassignedEmployees", 1L);
    }

    @Test
    void rejectsArbitraryBrowserSuppliedFieldNames() {
        var request = new ReportQueryRequest(
                List.of("password"), List.of(), List.of(), 0, 25);

        assertThatThrownBy(() -> service.query(request))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessage("Unsupported report field: password");
    }
}
