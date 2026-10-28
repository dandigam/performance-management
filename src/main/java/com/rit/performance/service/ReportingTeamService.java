package com.rit.performance.service;

import com.rit.performance.dto.ReportingTeamResponse;
import com.rit.performance.repository.EmployeeOrgHierarchyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportingTeamService {
    private final CurrentEmployeeService currentEmployeeService;
    private final EmployeeOrgHierarchyRepository hierarchyRepository;

    @Transactional(readOnly = true)
    public ReportingTeamResponse getMyReportingTeam() {
        var manager = currentEmployeeService.currentEmployee();
        var employees = hierarchyRepository.findReportingTeam(manager.getId());
        String managerName = ((manager.getFirstName() == null ? "" : manager.getFirstName())
                + " " + (manager.getLastName() == null ? "" : manager.getLastName())).trim();
        return new ReportingTeamResponse(manager.getId(), managerName, employees.size(), employees);
    }
}
