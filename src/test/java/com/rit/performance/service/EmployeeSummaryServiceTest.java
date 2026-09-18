package com.rit.performance.service;

import com.rit.performance.entity.*;
import com.rit.performance.repository.*;
import com.rit.performance.exception.InvalidOperationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import java.time.LocalDate;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeSummaryServiceTest {
    @Mock EmployeeRepository employeeRepository;
    @Mock EmployeeAssignmentRepository assignmentRepository;
    @Mock SowRepository sowRepository;
    @Mock LookupValueRepository lookupValueRepository;
    @InjectMocks EmployeeSummaryService service;

    @Test void returnsCompactPageWithDeduplicatedProjectsAndDepartment() {
        var employee = new Employee();
        employee.setId(2L);
        employee.setFirstName("Charan");
        employee.setLastName("Kovvuru");
        employee.setDesignationId(21L);
        var designation = LookupValue.builder().id(21L).name("Technical Lead").build();
        var department = LookupValue.builder().id(7L).name("Engineering").build();
        var sow = Sow.builder().id(20L).sowName("CBMS Support").businessUnit(department).build();
        var assignment = new EmployeeAssignment();
        assignment.setEmployeeId(2L);
        assignment.setSowId(20L);
        when(employeeRepository.findSummaries(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any(LocalDate.class), any(Pageable.class))).thenReturn(
                new PageImpl<>(List.of(employee), PageRequest.of(0, 20), 25));
        when(assignmentRepository.findCurrentForEmployees(eq(List.of(2L)), any(LocalDate.class)))
                .thenReturn(List.of(assignment, assignment));
        when(sowRepository.findAllById(any())).thenReturn(List.of(sow));
        when(lookupValueRepository.findAllById(any())).thenReturn(List.of(designation));

        var response = service.getSummaries(0, 20);

        assertThat(response.totalElements()).isEqualTo(25);
        assertThat(response.totalPages()).isEqualTo(2);
        assertThat(response.first()).isTrue();
        assertThat(response.last()).isFalse();
        var result = response.content().get(0);
        assertThat(result.employeeName()).isEqualTo("Charan Kovvuru");
        assertThat(result.designationName()).isEqualTo("Technical Lead");
        assertThat(result.departmentId()).isEqualTo(7L);
        assertThat(result.currentProjects()).hasSize(1);
        assertThat(result.currentProjects().get(0).sowId()).isEqualTo(20L);
    }

    @Test void emptyPageDoesNotLoadAssignments() {
        when(employeeRepository.findSummaries(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any(LocalDate.class), any(Pageable.class))).thenReturn(Page.empty(PageRequest.of(0, 20)));
        assertThat(service.getSummaries(0, 20).content()).isEmpty();
        verifyNoInteractions(assignmentRepository, sowRepository, lookupValueRepository);
    }

    @Test void rejectsInvalidPagination() {
        assertThatThrownBy(() -> service.getSummaries(-1, 20)).isInstanceOf(InvalidOperationException.class);
        assertThatThrownBy(() -> service.getSummaries(0, 0)).isInstanceOf(InvalidOperationException.class);
        assertThatThrownBy(() -> service.getSummaries(0, 101)).isInstanceOf(InvalidOperationException.class);
        verifyNoInteractions(employeeRepository);
    }

    @Test void normalizesFiltersAndUsesStableDescendingNameSort() {
        when(employeeRepository.findSummaries(eq("%charan!_!%%"), eq(5L), eq(20L),
                eq("ASSIGNED"), eq("ONSITE"), eq("ACTIVE"), any(LocalDate.class), any(Pageable.class)))
                .thenReturn(Page.empty(PageRequest.of(0, 20)));
        service.getSummaries(0, 20, " Charan_% ", 5L, 20L, "assigned", "onsite", "active", "employeeName,desc");
        var pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(employeeRepository).findSummaries(eq("%charan!_!%%"), eq(5L), eq(20L),
                eq("ASSIGNED"), eq("ONSITE"), eq("ACTIVE"), any(LocalDate.class), pageable.capture());
        assertThat(pageable.getValue().getSort().getOrderFor("firstName").getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(pageable.getValue().getSort().getOrderFor("id")).isNotNull();
    }

    @Test void rejectsUnknownFiltersAndUnsafeSort() {
        assertThatThrownBy(() -> service.getSummaries(0, 20, null, null, null,
                "COMPLETED", null, null, null)).isInstanceOf(InvalidOperationException.class);
        assertThatThrownBy(() -> service.getSummaries(0, 20, null, null, null,
                null, null, null, "unknown,asc")).isInstanceOf(InvalidOperationException.class);
        assertThatThrownBy(() -> service.getSummaries(0, 20, null, null, null,
                null, null, null, "employeeName,sideways")).isInstanceOf(InvalidOperationException.class);
        verifyNoInteractions(employeeRepository);
    }
}
