package com.rit.performance.dto.response;

import java.time.LocalDateTime;

public record EmployeeOrgHierarchyResponse(Long id, Long employeeId, String employeeName,
        Long reportingManagerId, String reportingManagerName,
        Long createdBy, LocalDateTime createdOn, Long updatedBy, LocalDateTime updatedOn) {
}
