package com.rit.performance.service.impl;

import com.rit.performance.dto.request.*;
import com.rit.performance.entity.*;
import com.rit.performance.exception.InvalidOperationException;
import com.rit.performance.exception.ResourceNotFoundException;
import com.rit.performance.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TimesheetEmployeeProjectServiceImplTest {
    private final TimesheetEmployeeProjectRepository projects = mock(TimesheetEmployeeProjectRepository.class);
    private final EmployeeRepository employees = mock(EmployeeRepository.class);
    private final SowRepository sows = mock(SowRepository.class);
    private final SowMilestoneRepository milestones = mock(SowMilestoneRepository.class);
    private final TimesheetProjectScheduleService schedules = mock(TimesheetProjectScheduleService.class);
    private final com.rit.performance.service.TimesheetGenerationService generation = mock(com.rit.performance.service.TimesheetGenerationService.class);
    private final PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
    private final SimpleTransactionStatus transaction = new SimpleTransactionStatus();
    private TimesheetEmployeeProjectServiceImpl service;

    @BeforeEach
    void setup() {
        TimesheetEmployeeProjectServiceImpl target = new TimesheetEmployeeProjectServiceImpl(projects,
                employees, sows, mock(SowMilestonePositionAssignmentRepository.class), milestones, schedules, generation);
        ProxyFactory proxy = new ProxyFactory(target);
        proxy.setProxyTargetClass(true);
        proxy.addAdvice(new TransactionInterceptor(transactions, new AnnotationTransactionAttributeSource()));
        service = (TimesheetEmployeeProjectServiceImpl) proxy.getProxy();
        when(transactions.getTransaction(any())).thenReturn(transaction);
        when(employees.findById(anyLong())).thenAnswer(call -> {
            Employee employee = new Employee(); employee.setId(call.getArgument(0)); employee.setFirstName("Test");
            return Optional.of(employee);
        });
        when(sows.findById(anyLong())).thenAnswer(call -> {
            Sow sow = new Sow(); sow.setId(call.getArgument(0)); return Optional.of(sow);
        });
        when(milestones.findByIdAndSow_Id(anyLong(), anyLong())).thenAnswer(call -> {
            SowMilestone milestone = new SowMilestone(); milestone.setId(call.getArgument(0));
            milestone.setStartDate(LocalDate.of(2026, 9, 1));
            milestone.setEndDate(LocalDate.of(2026, 9, 30));
            return Optional.of(milestone);
        });
        when(projects.saveAll(any())).thenAnswer(call -> {
            List<TimesheetEmployeeProject> assignments = call.getArgument(0);
            for (TimesheetEmployeeProject assignment : assignments)
                if (assignment.getId() == null) assignment.setId(123L);
            return assignments;
        });
    }

    @Test
    void createsHeadersAfterSchedulesAndRollsBackIfGenerationFails() {
        var request = request(null, 67L);
        var date = new TimesheetScheduleDateRequest();
        date.setWorkDate(LocalDate.of(2026, 9, 1));
        date.setScheduledHours(java.math.BigDecimal.ONE);
        request.setScheduleDates(List.of(date));
        doThrow(new InvalidOperationException("Header insert failed"))
                .when(generation).ensureWeeklyTimesheets(eq(3L), any());
        assertThatThrownBy(() -> service.create(3L, List.of(request)))
                .isInstanceOf(InvalidOperationException.class);
        var order = inOrder(schedules, generation);
        order.verify(schedules).applyChanges(any(), same(request));
        order.verify(generation).ensureWeeklyTimesheets(3L, List.of(date.getWorkDate()));
        verify(transactions).rollback(transaction);
        verify(transactions, never()).commit(any());
    }

    @Test
    void createsWithoutAssignmentIdAndEmptyDeletionsInOneTransaction() {
        TimesheetEmployeeProjectRequest request = request(null, 67L);
        assertThat(service.create(3L, List.of(request))).hasSize(1);
        verify(schedules).applyChanges(argThat(project -> project.getId().equals(123L)), same(request));
        verify(transactions).getTransaction(any());
        verify(transactions).commit(transaction);
        verify(transactions, never()).rollback(any());
    }

    @Test
    void cleanupFailureRollsBackScheduleChanges() {
        existing(123L, 67L, 3L);
        var request = request(123L, 67L);
        var deleted = new TimesheetDeletedDateRequest();
        deleted.setWorkDate(LocalDate.of(2026, 9, 30));
        request.setDeletedDates(List.of(deleted));
        doThrow(new InvalidOperationException("Cleanup failed"))
                .when(generation).cleanupEmptyDraftWeeks(eq(3L), any());
        assertThatThrownBy(() -> service.create(3L, List.of(request)))
                .isInstanceOf(InvalidOperationException.class);
        var order = inOrder(schedules, generation);
        order.verify(schedules).applyChanges(any(), same(request));
        order.verify(generation).ensureWeeklyTimesheets(3L, List.of());
        order.verify(generation).cleanupEmptyDraftWeeks(3L, List.of(deleted.getWorkDate()));
        verify(transactions).rollback(transaction);
        verify(transactions, never()).commit(any());
    }

    @Test
    void acceptsDeletionOnlyUpdate() {
        existing(123L, 67L, 3L);
        TimesheetEmployeeProjectRequest request = request(123L, 67L);
        TimesheetDeletedDateRequest deleted = new TimesheetDeletedDateRequest();
        deleted.setWorkDate(LocalDate.of(2026, 9, 3));
        request.setDeletedDates(List.of(deleted));
        service.create(3L, List.of(request));
        verify(schedules).applyChanges(any(), same(request));
        verify(generation).cleanupEmptyDraftWeeks(3L, List.of(deleted.getWorkDate()));
        verify(transactions).commit(transaction);
    }

    @Test
    void failureInSecondAssignmentRollsBackWholeBatch() {
        existing(123L, 67L, 3L);
        existing(124L, 68L, 3L);
        TimesheetEmployeeProjectRequest first = request(123L, 67L);
        TimesheetEmployeeProjectRequest second = request(124L, 68L);
        doThrow(new InvalidOperationException("Invalid schedule date"))
                .when(schedules).applyChanges(any(), same(second));

        assertThatThrownBy(() -> service.create(3L, List.of(first, second)))
                .isInstanceOf(InvalidOperationException.class);

        verify(schedules).applyChanges(any(), same(first));
        verify(transactions, times(1)).getTransaction(any());
        verify(transactions).rollback(transaction);
        verify(transactions, never()).commit(any());
    }

    @Test
    void rejectsAssignmentBelongingToAnotherEmployee() {
        existing(123L, 67L, 99L);
        assertThatThrownBy(() -> service.create(3L, List.of(request(123L, 67L))))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(schedules);
    }

    @Test
    void rejectsAssignmentWithDifferentMilestone() {
        existing(123L, 67L, 3L);
        assertThatThrownBy(() -> service.create(3L, List.of(request(123L, 68L))))
                .isInstanceOf(InvalidOperationException.class).hasMessageContaining("must match");
        verifyNoInteractions(schedules);
    }

    @Test
    void rejectsDeletionsOnCreation() {
        TimesheetEmployeeProjectRequest request = request(null, 67L);
        TimesheetDeletedDateRequest deleted = new TimesheetDeletedDateRequest();
        deleted.setWorkDate(LocalDate.of(2026, 9, 3));
        request.setDeletedDates(List.of(deleted));
        assertThatThrownBy(() -> service.create(3L, List.of(request)))
                .isInstanceOf(InvalidOperationException.class).hasMessageContaining("creating");
        verify(projects, never()).saveAll(any());
    }

    @Test
    void rejectsLegacyReplacementPayload() {
        TimesheetEmployeeProjectRequest request = request(null, 67L);
        request.setDailyOverrides(List.of());
        assertThatThrownBy(() -> service.create(3L, List.of(request)))
                .isInstanceOf(InvalidOperationException.class).hasMessageContaining("scheduleDates and deletedDates");
    }

    private void existing(Long id, Long milestoneId, Long employeeId) {
        Employee employee = new Employee(); employee.setId(employeeId); employee.setFirstName("Test");
        Sow sow = new Sow(); sow.setId(45L);
        SowMilestone milestone = new SowMilestone(); milestone.setId(milestoneId);
        TimesheetEmployeeProject project = new TimesheetEmployeeProject();
        project.setId(id); project.setEmployee(employee); project.setSow(sow); project.setMilestone(milestone);
        when(projects.findById(id)).thenReturn(Optional.of(project));
    }

    private TimesheetEmployeeProjectRequest request(Long id, Long milestoneId) {
        TimesheetEmployeeProjectRequest request = new TimesheetEmployeeProjectRequest();
        request.setTimesheetEmployeeProjectId(id); request.setSowId(45L); request.setMilestoneId(milestoneId);
        request.setStartDate(LocalDate.of(2026, 9, 1)); request.setEndDate(LocalDate.of(2026, 9, 30));
        request.setLevel1ApproverId(10L); request.setLevel2ApproverId(20L);
        request.setStatus(TimesheetEmployeeProjectStatus.ACTIVE);
        return request;
    }
}


