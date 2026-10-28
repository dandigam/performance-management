package com.rit.performance.service;

import com.rit.performance.dto.TeamPerformanceReviewsResponse;
import com.rit.performance.entity.Employee;
import com.rit.performance.entity.EmployeeReviewAssessment;
import com.rit.performance.entity.LookupValue;
import com.rit.performance.exception.InvalidOperationException;
import com.rit.performance.exception.ResourceNotFoundException;
import com.rit.performance.repository.EmployeeReviewAssessmentRepository;
import com.rit.performance.repository.LookupValueRepository;
import com.rit.performance.repository.PerformanceCycleConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeamPerformanceReviewsService {
    private final CurrentEmployeeService currentEmployeeService;
    private final PerformanceCycleConfigRepository cycles;
    private final EmployeeReviewAssessmentRepository assessments;
    private final LookupValueRepository lookups;

    @Transactional(readOnly = true)
    public TeamPerformanceReviewsResponse getMyTeamReviews(Long cycleId) {
        Employee reviewer = currentEmployeeService.currentEmployee();
        if (cycleId == null || cycleId <= 0)
            throw new InvalidOperationException("cycleId must be positive");
        var cycle = cycles.findById(cycleId)
                .orElseThrow(() -> new ResourceNotFoundException("Performance cycle not found: " + cycleId));
        var assigned = assessments.findAssignedTeamAssessments(reviewer.getId(), cycleId).stream()
                .filter(a -> Set.of("REPORTING_MANAGER_1", "REPORTING_MANAGER_2")
                        .contains(AssessmentAssigneeResolver.stageCode(a.getAssessorRole())))
                .toList();
        var designationIds = assigned.stream()
                .map(a -> a.getEmployeeReview().getEmployee().getDesignationId())
                .filter(Objects::nonNull).distinct().toList();
        Map<Long, LookupValue> designations = lookups.findAllById(designationIds).stream()
                .collect(Collectors.toMap(LookupValue::getId, Function.identity()));
        var reviews = assigned.stream().map(a -> {
            var review = a.getEmployeeReview();
            var employee = review.getEmployee();
            var designation = designations.get(employee.getDesignationId());
            return new TeamPerformanceReviewsResponse.Review(employee.getId(), name(employee),
                    employee.getRitId(), designation == null ? null : designation.getName(),
                    review.getId(), a.getId(), AssessmentAssigneeResolver.stageCode(a.getAssessorRole()),
                    a.getStatus(), action(a));
        }).toList();
        return new TeamPerformanceReviewsResponse(reviewer.getId(), name(reviewer),
                cycle.getId(), cycle.getCycleName(), reviews);
    }

    private String action(EmployeeReviewAssessment assessment) {
        if (assessment.getStatus() == EmployeeReviewStatus.SUBMITTED) return "VIEW";
        boolean ready = assessment.getEmployeeReview().getAssessments().stream()
                .filter(a -> a.getAssessmentLevel() < assessment.getAssessmentLevel())
                .allMatch(a -> a.getStatus() == EmployeeReviewStatus.SUBMITTED);
        if (!ready) return "VIEW";
        return assessment.getStatus() == EmployeeReviewStatus.NOT_STARTED ? "START" : "CONTINUE";
    }

    private String name(Employee employee) {
        return ((employee.getFirstName() == null ? "" : employee.getFirstName())
                + " " + (employee.getLastName() == null ? "" : employee.getLastName())).trim();
    }
}
