package com.rit.performance.controller;

import com.rit.performance.dto.EmployeeHierarchyMemberResponse;
import com.rit.performance.service.CurrentEmployeeService;
import com.rit.performance.service.EmployeeService;
import com.rit.performance.service.ReportingTeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/employee-reviews/me")
public class TeamAssessmentsController {
    private final CurrentEmployeeService currentEmployeeService;
    private final EmployeeService employeeService;
    private final ReportingTeamService reportingTeamService;

    @GetMapping("/team-assessments")
    @Transactional(readOnly = true)
    public TeamAssessmentsResponse getTeamAssessments(
            @RequestParam Long cycleId,
            @RequestParam(defaultValue = "REPORTING_MANAGER_1") String roleType) {
        var viewer = currentEmployeeService.currentEmployee();
        String role = roleType.trim().toUpperCase(Locale.ROOT);
        if (cycleId <= 0 || !List.of("REPORTING_MANAGER_1", "REPORTING_MANAGER_2").contains(role)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A positive cycleId and reporting manager responsibility are required.");
        }
        var assignments = employeeService.getHierarchy(viewer.getId(), role, cycleId);
        Map<Long, TeamAssessmentEmployee> employees = new LinkedHashMap<>();
        if ("REPORTING_MANAGER_1".equals(role)) {
            var team = reportingTeamService.getMyReportingTeam();
            for (var employee : team.employees()) {
                employees.put(employee.employeeId(), new TeamAssessmentEmployee(
                        employee.employeeId(), employee.employeeName(), employee.ritId(),
                        employee.designationName(), null, null, team.managerEmployeeName(),
                        null, null, null, null, false, true));
            }
        }
        for (var member : assignments.getEmployees()) {
            boolean currentDirectReport = employees.containsKey(member.getEmployeeId());
            employees.put(member.getEmployeeId(), fromAssignment(member, currentDirectReport));
        }
        var sorted = employees.values().stream()
                .sorted(Comparator.comparing(TeamAssessmentEmployee::employeeName,
                        String.CASE_INSENSITIVE_ORDER)).toList();
        return new TeamAssessmentsResponse(viewer.getId(), assignments.getViewerEmployeeName(),
                cycleId, assignments.getCycleName(), role, sorted.size(), sorted);
    }

    private TeamAssessmentEmployee fromAssignment(EmployeeHierarchyMemberResponse member, boolean currentDirectReport) {
        return new TeamAssessmentEmployee(member.getEmployeeId(), member.getEmployeeName(),
                member.getRitId(), member.getDesignationName(), member.getDepartmentName(),
                member.getSowName(), member.getReportingManager1Name(), member.getReviewId(),
                member.getAssignedAssessmentId(), member.getAssignedRoleName(),
                member.getAssignedAssessmentStatus() == null ? null : member.getAssignedAssessmentStatus().name(),
                member.isActionRequired(), currentDirectReport);
    }

    public record TeamAssessmentsResponse(Long viewerEmployeeId, String viewerEmployeeName,
            Long cycleId, String cycleName, String roleType, int totalEmployees,
            List<TeamAssessmentEmployee> employees) {}

    public record TeamAssessmentEmployee(Long employeeId, String employeeName, String ritId,
            String designationName, String departmentName, String projectName,
            String reportingManager1Name, Long reviewId, Long assignedAssessmentId,
            String assignedRoleName, String assignedAssessmentStatus,
            boolean actionRequired, boolean currentDirectReport) {}
}
