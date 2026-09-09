package com.rit.performance.service.impl;

import com.rit.performance.dto.response.TimesheetWeekResponse;
import com.rit.performance.entity.Employee;
import com.rit.performance.entity.Sow;
import com.rit.performance.entity.Timesheet;
import com.rit.performance.entity.TimesheetEmployeeProject;
import com.rit.performance.entity.TimesheetEmployeeProjectStatus;
import com.rit.performance.repository.EmployeeRepository;
import com.rit.performance.repository.HolidayRepository;
import com.rit.performance.repository.SowMilestonePositionAssignmentRepository;
import com.rit.performance.repository.TimesheetEmployeeProjectRepository;
import com.rit.performance.repository.TimesheetRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TimesheetGenerationServiceImplTest {

    @Test
    void getWeekIncludesInactiveProjectThatOverlappedTimesheetWeek() {
        TimesheetRepository timesheetRepository = mock(TimesheetRepository.class);
        TimesheetEmployeeProjectRepository projectRepository =
                mock(TimesheetEmployeeProjectRepository.class);
        EmployeeRepository employeeRepository = mock(EmployeeRepository.class);
        HolidayRepository holidayRepository = mock(HolidayRepository.class);
        SowMilestonePositionAssignmentRepository milestoneAssignmentRepository =
                mock(SowMilestonePositionAssignmentRepository.class);
        TimesheetGenerationServiceImpl service = new TimesheetGenerationServiceImpl(
                timesheetRepository, projectRepository, employeeRepository,
                holidayRepository, milestoneAssignmentRepository);

        long employeeId = 10L;
        long timesheetId = 20L;
        LocalDate weekStart = LocalDate.of(2026, 8, 30);
        LocalDate weekEnd = weekStart.plusDays(6);

        Employee employee = new Employee();
        employee.setId(employeeId);

        Timesheet timesheet = new Timesheet();
        timesheet.setId(timesheetId);
        timesheet.setEmployee(employee);
        timesheet.setWeekStartDate(weekStart);
        timesheet.setWeekEndDate(weekEnd);

        Sow sow = new Sow();
        sow.setId(30L);
        sow.setSowCode("PROJECT-A");
        sow.setSowName("Project A");

        TimesheetEmployeeProject previousProject = new TimesheetEmployeeProject();
        previousProject.setId(40L);
        previousProject.setEmployee(employee);
        previousProject.setSow(sow);
        previousProject.setStartDate(weekStart.minusMonths(1));
        previousProject.setEndDate(weekEnd);
        previousProject.setMaxHoursPerDay(8);
        previousProject.setStatus(TimesheetEmployeeProjectStatus.INACTIVE);

        when(timesheetRepository.findOneById(timesheetId)).thenReturn(Optional.of(timesheet));
        when(projectRepository.findByEmployeeIdOrderByStartDateDescIdDesc(employeeId))
                .thenReturn(List.of(previousProject));
        when(milestoneAssignmentRepository
                .findByEmployeeAssignment_EmployeeIdOrderByAssignmentStartDateDescIdDesc(employeeId))
                .thenReturn(List.of());
        when(holidayRepository.findByHolidayDateBetweenOrderByHolidayDateAsc(weekStart, weekEnd))
                .thenReturn(List.of());

        TimesheetWeekResponse response = service.getWeek(employeeId, weekStart, timesheetId);

        assertThat(response.getProjects()).hasSize(1);
        assertThat(response.getProjects().get(0).getTimesheetEmployeeProjectId())
                .isEqualTo(previousProject.getId());
        assertThat(response.getProjects().get(0).getSowId()).isEqualTo(sow.getId());
    }
}
