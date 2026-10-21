package com.rit.performance.dto.response;

public record EmployeeLeaveSetupResponse(Long employeeId, String employeeNumber, String employeeName,
        Long employeeLeavePolicyId, Long leavePolicyId, String policyName, String setupStatus) {}
