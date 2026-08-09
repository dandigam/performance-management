package com.rit.performance.service;

import com.rit.performance.dto.EmployeeBasicInfoResponse;
import com.rit.performance.dto.EmployeeCreateRequest;
import com.rit.performance.dto.EmployeeCreateResponse;
import com.rit.performance.dto.EmployeeUpdateRequest;
import com.rit.performance.dto.ReportingManagerResponse;
import com.rit.performance.dto.DirectReportsResponse;
import com.rit.performance.dto.EmployeeHierarchyResponse;
import com.rit.performance.dto.EmployeeInformationResponse;

import java.util.List;

public interface EmployeeService {
    EmployeeCreateResponse create(EmployeeCreateRequest request);

    List<EmployeeBasicInfoResponse> getBasicInfo();

    EmployeeBasicInfoResponse getById(Long employeeId);

    List<EmployeeInformationResponse> getEmployeeInformation();

    EmployeeBasicInfoResponse update(Long employeeId, EmployeeUpdateRequest request);

    List<ReportingManagerResponse> getReportingManagers(
            Long projectId, Long departmentId, Long designationId, Long excludeEmployeeId);

    DirectReportsResponse getDirectReports(Long managerEmployeeId);

    EmployeeHierarchyResponse getHierarchy(Long employeeId, String roleType, Long cycleId);
}
