package com.rit.performance.service.impl;

import com.rit.performance.entity.*;
import com.rit.performance.repository.*;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class TimesheetAuditLogTest {
    private final TimesheetRepository repository = mock(TimesheetRepository.class);
    private final TimesheetGenerationServiceImpl service = new TimesheetGenerationServiceImpl(
            repository, Clock.fixed(Instant.parse("2026-09-16T12:00:00Z"), ZoneOffset.UTC),
            mock(TimesheetEmployeeProjectRepository.class), mock(EmployeeRepository.class),
            mock(SowMilestonePositionAssignmentRepository.class));
    private final Timesheet sheet = new Timesheet();

    TimesheetAuditLogTest() {
        sheet.setEmployee(employee(2L, "Worker"));
        sheet.setWeekStartDate(LocalDate.of(2026, 8, 30));
        sheet.setWeekEndDate(LocalDate.of(2026, 9, 5));
        when(repository.findForStatusTab(isNull(), any(), anyList())).thenReturn(List.of(sheet));
    }

    private Employee employee(Long id, String name) {
        var employee = new Employee(); employee.setId(id); employee.setFirstName(name); return employee;
    }

    @Test void returnsCompletedActionsNewestFirstWithActorsAndComments() {
        sheet.setSubmittedAt(LocalDateTime.of(2026, 9, 7, 7, 44));
        sheet.setStatus(TimesheetStatus.APPROVED);
        sheet.addApproval(TimesheetApproval.builder().approvalLevel(1)
                .approverEmployee(employee(3L, "Primary approver"))
                .status(TimesheetApprovalStatus.APPROVED)
                .actionAt(LocalDateTime.of(2026, 9, 8, 14, 20)).comments("Checked hours").build());
        sheet.addApproval(TimesheetApproval.builder().approvalLevel(2)
                .approverEmployee(employee(4L, "Secondary approver"))
                .status(TimesheetApprovalStatus.APPROVED)
                .actionAt(LocalDateTime.of(2026, 9, 10, 13, 52)).build());
        var events = service.getAll(null, "ALL").get(0).getAuditLog();
        assertThat(events).extracting(e -> e.stage()).containsExactly("Secondary", "Primary", "Submitted");
        assertThat(events).extracting(e -> e.employeeId()).containsExactly(4L, 3L, 2L);
        assertThat(events.get(0).actionAt()).isEqualTo(LocalDateTime.of(2026, 9, 10, 13, 52));
        assertThat(events.get(1).comments()).isEqualTo("Checked hours");
        assertThat(events.get(2).action()).isEqualTo("SUBMITTED");
        assertThat(events.get(2).employeeName()).isEqualTo("Worker");
    }

    @Test void draftHasEmptyLogAndBlankStatusUsesAll() {
        assertThat(service.getAll(null, " ").get(0).getAuditLog()).isEmpty();
        verify(repository).findForStatusTab(isNull(), any(), argThat(statuses ->
                statuses.contains(TimesheetStatus.APPROVED) && statuses.contains(TimesheetStatus.DRAFT)
                        && !statuses.contains(TimesheetStatus.CANCELLED)));
    }

    @Test void excludesPendingAndPreservesRejectionWithUnknownTimestamp() {
        sheet.setSubmittedAt(LocalDateTime.of(2026, 9, 7, 7, 44));
        sheet.addApproval(TimesheetApproval.builder().approvalLevel(2)
                .approverEmployee(employee(4L, "Secondary approver")).build());
        sheet.addApproval(TimesheetApproval.builder().approvalLevel(1)
                .approverEmployee(employee(3L, "Primary approver"))
                .status(TimesheetApprovalStatus.REJECTED).comments("Correct hours").build());
        var events = service.getAll(null, "ALL").get(0).getAuditLog();
        assertThat(events).hasSize(2);
        assertThat(events.get(1).action()).isEqualTo("REJECTED");
        assertThat(events.get(1).actionAt()).isNull();
        assertThat(events.get(1).comments()).isEqualTo("Correct hours");
    }
}
