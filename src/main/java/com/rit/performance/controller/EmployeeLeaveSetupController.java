package com.rit.performance.controller;

import com.rit.performance.dto.response.EmployeeLeaveSetupPageResponse;
import com.rit.performance.service.EmployeeLeaveSetupService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/employees/leave-setup")
public class EmployeeLeaveSetupController {
    private final EmployeeLeaveSetupService service;

    @GetMapping
    public EmployeeLeaveSetupPageResponse list(@RequestParam int year,
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.list(year, status, search, page, size);
    }
}
