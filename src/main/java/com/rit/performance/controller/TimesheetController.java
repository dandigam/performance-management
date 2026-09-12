package com.rit.performance.controller;

import com.rit.performance.dto.response.TimesheetSummaryResponse;
import com.rit.performance.dto.response.TimesheetWeekResponse;
import com.rit.performance.service.TimesheetGenerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/timesheets")
@RequiredArgsConstructor
public class TimesheetController {
    private final TimesheetGenerationService service;

    @GetMapping
    public ResponseEntity<List<TimesheetSummaryResponse>> getAll(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(defaultValue = "ALL") String status) {
        return ResponseEntity.ok(service.getAll(employeeId, status));
    }

    @GetMapping("/week")
    public ResponseEntity<TimesheetWeekResponse> getWeek(
            @RequestParam Long employeeId,
            @RequestParam LocalDate weekStart,
            @RequestParam(required = false) Long timesheetId) {
        return ResponseEntity.ok(service.getWeek(employeeId, weekStart, timesheetId));
    }

}
