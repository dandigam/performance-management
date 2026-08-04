package com.rit.performance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DirectReportsResponse {
    private Long managerEmployeeId;
    private String managerEmployeeName;
    private int totalReports;
    private List<EmployeeBasicInfoResponse> employees;
}
