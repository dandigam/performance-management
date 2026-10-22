package com.rit.performance.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmployeeOrgHierarchyRequest {
    @NotNull
    @Positive
    private Long employeeId;

    @Positive
    private Long reportingManagerId;
}
