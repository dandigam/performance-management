package com.rit.performance.service;

import com.rit.performance.dto.EmployeeSelfProfileResponse;
import com.rit.performance.dto.DocumentResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmployeeSelfProfileService {
    private final CurrentEmployeeService currentEmployeeService;
    private final EmployeeService employeeService;

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public EmployeeSelfProfileResponse getMyProfile() {
        Long employeeId = currentEmployeeService.currentEmployee().getId();
        return EmployeeSelfProfileResponse.from(employeeService.getById(employeeId));
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public List<DocumentResponse> getMyDocuments() {
        Long employeeId = currentEmployeeService.currentEmployee().getId();
        return employeeService.getDocuments(employeeId);
    }
}
