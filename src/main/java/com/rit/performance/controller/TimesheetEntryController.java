package com.rit.performance.controller;

import com.rit.performance.dto.request.TimesheetEntriesRequest;
import com.rit.performance.dto.response.TimesheetEntriesResponse;
import com.rit.performance.service.impl.TimesheetEntryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/timesheets")
public class TimesheetEntryController {
    private final TimesheetEntryService service;

    @PostMapping("/{timesheetId}/entries")
    public TimesheetEntriesResponse save(@PathVariable Long timesheetId,
                                        @RequestBody TimesheetEntriesRequest request) {
        return service.save(timesheetId, request);
    }
}
