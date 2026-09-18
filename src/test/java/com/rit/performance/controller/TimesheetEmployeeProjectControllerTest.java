package com.rit.performance.controller;

import com.rit.performance.dto.request.TimesheetEmployeeProjectRequest;
import com.rit.performance.dto.response.TimesheetEmployeeProjectResponse;
import com.rit.performance.dto.response.TimesheetDailyOverrideResponse;
import com.rit.performance.entity.TimesheetDayType;
import com.rit.performance.exception.GlobalExceptionHandler;
import com.rit.performance.service.TimesheetEmployeeProjectService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TimesheetEmployeeProjectControllerTest {
    private final TimesheetEmployeeProjectService service = mock(TimesheetEmployeeProjectService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new TimesheetEmployeeProjectController(service))
            .setControllerAdvice(new GlobalExceptionHandler()).build();

    @Test
    void detailReturnsScheduleDatesInsteadOfDailyOverrides() throws Exception {
        when(service.get(3L, 11L, 89L)).thenReturn(TimesheetEmployeeProjectResponse.builder()
                .timesheetEmployeeProjectId(4L)
                .startDate(LocalDate.of(2026, 10, 1)).endDate(LocalDate.of(2026, 12, 31))
                .assignmentStartDate(LocalDate.of(2026, 9, 1))
                .scheduleDates(List.of(TimesheetDailyOverrideResponse.builder()
                        .workDate(LocalDate.of(2026, 10, 1))
                        .scheduledHours(new BigDecimal("2.00"))
                        .dayType(TimesheetDayType.WORKING_DAY)
                        .build()))
                .build());

        mvc.perform(get("/api/v1/employees/3/timesheet-projects/11/milestones/89"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timesheetEmployeeProjectId").value(4))
                .andExpect(jsonPath("$.plannedStartDate").value("2026-10-01"))
                .andExpect(jsonPath("$.plannedEndDate").value("2026-12-31"))
                .andExpect(jsonPath("$.assignmentStartDate").value("2026-09-01"))
                .andExpect(jsonPath("$.assignmentEndDate").doesNotExist())
                .andExpect(jsonPath("$.scheduleDates.length()").value(1))
                .andExpect(jsonPath("$.scheduleDates[0].workDate").value("2026-10-01"))
                .andExpect(jsonPath("$.scheduleDates[0].scheduledHours").value(2.0))
                .andExpect(jsonPath("$.scheduleDates[0].dayType").value("WORKING_DAY"))
                .andExpect(jsonPath("$.dailyOverrides").doesNotExist());
    }

    @Test void acceptsExplicitPlannedDatesAndAssignmentLink() throws Exception {
        mvc.perform(post("/api/v1/employees/3/timesheet-projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload("[]", "[]").replace("\"startDate\"", "\"plannedStartDate\"")
                        .replace("\"endDate\"", "\"plannedEndDate\"")
                        .replace("\"sowId\":45", "\"milestonePositionAssignmentId\":7,\"sowId\":45")))
                .andExpect(status().isOk());
        ArgumentCaptor<List<TimesheetEmployeeProjectRequest>> captured = ArgumentCaptor.forClass(List.class);
        verify(service).create(eq(3L), captured.capture());
        assertThat(captured.getValue().get(0).getStartDate()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(captured.getValue().get(0).getMilestonePositionAssignmentId()).isEqualTo(7L);
    }

    private String payload(String scheduleDates, String deletedDates) {
        return """
                [{"timesheetEmployeeProjectId":123,"sowId":45,"milestoneId":67,
                  "startDate":"2026-09-01","endDate":"2026-09-30","defaultHoursPerDay":null,
                  "scheduleDates":%s,"deletedDates":%s,
                  "level1ApproverId":10,"level2ApproverId":20,"status":"ACTIVE"}]
                """.formatted(scheduleDates, deletedDates);
    }

    @Test
    void acceptsProposedPayloadOnExistingPostEndpoint() throws Exception {
        mvc.perform(post("/api/v1/employees/3/timesheet-projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload("""
                        [{"workDate":"2026-09-01","scheduledHours":8},
                         {"workDate":"2026-09-02","scheduledHours":6.5}]
                        """, "[{\"workDate\":\"2026-09-03\"}]")))
                .andExpect(status().isOk());
        ArgumentCaptor<List<TimesheetEmployeeProjectRequest>> captured = ArgumentCaptor.forClass(List.class);
        verify(service).create(eq(3L), captured.capture());
        TimesheetEmployeeProjectRequest request = captured.getValue().get(0);
        assertThat(request.getTimesheetEmployeeProjectId()).isEqualTo(123L);
        assertThat(request.getScheduleDates()).hasSize(2);
        assertThat(request.getScheduleDates().get(1).getScheduledHours()).isEqualByComparingTo("6.5");
        assertThat(request.getDeletedDates().get(0).getWorkDate()).hasToString("2026-09-03");
    }

    @Test
    void rejectsScheduleDateWithoutHours() throws Exception {
        mvc.perform(post("/api/v1/employees/3/timesheet-projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload("[{\"workDate\":\"2026-09-01\"}]", "[]")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void rejectsDeletedDateWithoutWorkDate() throws Exception {
        mvc.perform(post("/api/v1/employees/3/timesheet-projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload("[]", "[{}]")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "null", "[null]", "[{\"scheduledHours\":8}]",
            "[{\"workDate\":\"2026-09-01\",\"scheduledHours\":-1}]",
            "[{\"workDate\":\"2026-09-01\",\"scheduledHours\":25}]"
    })
    void rejectsInvalidScheduleEntries(String scheduleDates) throws Exception {
        mvc.perform(post("/api/v1/employees/3/timesheet-projects")
                .contentType(MediaType.APPLICATION_JSON).content(payload(scheduleDates, "[]")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"[]", "[null]"})
    void rejectsEmptyOrNullAssignments(String body) throws Exception {
        mvc.perform(post("/api/v1/employees/3/timesheet-projects")
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
