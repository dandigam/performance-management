package com.rit.performance.dto.response;

import java.util.List;

public record EmployeeLeaveSetupPageResponse(List<EmployeeLeaveSetupResponse> content,
        long totalElements, int totalPages, int number, int size) {}
