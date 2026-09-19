package com.rit.performance.service;

import com.rit.performance.dto.response.TimesheetSummaryResponse;
import com.rit.performance.dto.response.TimesheetWeekResponse;
import com.rit.performance.dto.response.TimesheetApprovalResponse;

import java.time.LocalDate;
import java.util.List;

public interface TimesheetGenerationService {
    void ensureWeeklyTimesheets(Long employeeId, java.util.Collection<LocalDate> scheduleDates);
    void cleanupEmptyDraftWeeks(Long employeeId, java.util.Collection<LocalDate> deletedDates);

    List<TimesheetSummaryResponse> getAll(Long employeeId, String status);

    List<TimesheetApprovalResponse> getApprovals(Long reviewerEmployeeId, String status);

    TimesheetWeekResponse getWeek(Long employeeId, LocalDate weekStart, Long timesheetId);


}
