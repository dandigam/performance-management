package com.rit.performance.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class EmployeeOrgHierarchyBulkRequest {
    @NotEmpty
    private List<@NotNull @Valid EmployeeEntry> employeeList;

    @Positive
    private Long reportingManagerId;

    @Getter
    @Setter
    public static class EmployeeEntry {
        @NotNull
        @Positive
        private Long employeeId;
    }
}
