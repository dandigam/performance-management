package com.rit.performance.service;

import com.rit.performance.dto.request.TimesheetGenerateRequest;
import com.rit.performance.dto.request.TimesheetHistoryGenerateRequest;
import com.rit.performance.dto.response.TimesheetGenerateResponse;
import com.rit.performance.dto.response.TimesheetSummaryResponse;
import com.rit.performance.dto.response.TimesheetWeekResponse;

import java.time.LocalDate;
import java.util.List;

public interface TimesheetGenerationService {
    TimesheetGenerateResponse generate(TimesheetGenerateRequest request);

    List<TimesheetSummaryResponse> getAll(Long employeeId);

    TimesheetWeekResponse getWeek(Long employeeId, LocalDate weekStart, Long timesheetId);

    String generatePreviousDatesTimesheets(TimesheetHistoryGenerateRequest request);

}
