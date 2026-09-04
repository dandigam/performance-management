package com.rit.performance.service;

import com.rit.performance.dto.EmployeeBasicInfoResponse;
import com.rit.performance.dto.EmployeeCreateRequest;
import com.rit.performance.dto.EmployeeCreateResponse;
import com.rit.performance.dto.EmployeeUpdateRequest;
import com.rit.performance.dto.ReportingManagerResponse;
import com.rit.performance.dto.DirectReportsResponse;
import com.rit.performance.dto.EmployeeHierarchyResponse;
import com.rit.performance.dto.EmployeeInformationResponse;
import com.rit.performance.dto.EmployeeAssignmentRequest;
import com.rit.performance.dto.EmployeeAssignmentsResponse;
import com.rit.performance.dto.EmployeeFinanceHistoryResponse;
import com.rit.performance.dto.EmployeeAuditHistoryResponse;
import com.rit.performance.dto.DocumentResponse;

import java.util.List;

public interface EmployeeService {
    EmployeeCreateResponse create(EmployeeCreateRequest request);

    List<EmployeeBasicInfoResponse> getBasicInfo();

    EmployeeBasicInfoResponse getById(Long employeeId);
    EmployeeAssignmentsResponse getAssignmentsByEmployeeId(Long employeeId);

    List<EmployeeFinanceHistoryResponse> getFinanceHistory(Long employeeId);
    List<EmployeeAuditHistoryResponse> getAuditHistory(Long employeeId);
    List<DocumentResponse> getDocuments(Long employeeId);

    List<EmployeeInformationResponse> getEmployeeInformation();

    EmployeeBasicInfoResponse update(Long employeeId, EmployeeUpdateRequest request);

    EmployeeBasicInfoResponse assign(EmployeeAssignmentRequest request);

    List<ReportingManagerResponse> getReportingManagers(
            Long sowId, Long departmentId, Long designationId, Long excludeEmployeeId);

    DirectReportsResponse getDirectReports(Long managerEmployeeId);

    EmployeeHierarchyResponse getHierarchy(Long employeeId, String roleType, Long cycleId);
}
