package com.rit.performance.service;

import com.rit.performance.dto.CyclePublishResponse;
import com.rit.performance.entity.*;
import com.rit.performance.repository.EmployeeRepository;
import com.rit.performance.repository.EmployeeAssignmentRepository;
import com.rit.performance.repository.EmployeeReviewRepository;
import com.rit.performance.repository.EmployeeReviewAssessmentRepository;
import com.rit.performance.repository.PerformanceCycleAssessorRepository;
import com.rit.performance.repository.UserRepository;
import com.rit.performance.repository.LookupValueRepository;
import com.rit.performance.repository.PerformanceCycleConfigRepository;
import com.rit.performance.repository.PerformanceCycleQuestionRepository;
import com.rit.performance.repository.PerformanceCycleSectionRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
@AllArgsConstructor
public class ReviewCyclePublishServiceImpl implements ReviewCyclePublishService {

    private final PerformanceCycleConfigRepository cycleRepository;
    private final PerformanceCycleSectionRepository sectionRepository;
    private final PerformanceCycleQuestionRepository questionRepository;
    private final LookupValueRepository lookupValueRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeAssignmentRepository employeeAssignmentRepository;
    private final EmployeeReviewRepository reviewRepository;
    private final EmployeeReviewAssessmentRepository assessmentRepository;
    private final PerformanceCycleAssessorRepository assessorConfigRepository;
    private final AssessmentAssigneeResolver assigneeResolver;
    private final EmailNotificationService emailNotificationService;


    @Override
    public CyclePublishResponse publish(Long cycleId, Long publishedBy) {
        PerformanceCycles cycle = cycleRepository.findById(cycleId)
                .orElseThrow(() -> new IllegalArgumentException("Performance cycle not found: " + cycleId));

        if (!"DRAFT".equalsIgnoreCase(cycle.getStatus())
                && !"ACTIVE".equalsIgnoreCase(cycle.getStatus())
                && !"PUBLISHED".equalsIgnoreCase(cycle.getStatus())) {
            throw new IllegalStateException("Only DRAFT, ACTIVE, or PUBLISHED cycles can be published");
        }

        validateConfiguration(cycle);
        List<Employee> eligibleEmployees = determineEligibleEmployees(cycle);
        if (eligibleEmployees.isEmpty()) {
            throw new IllegalStateException("No eligible employees found for this cycle");
        }

        List<EmployeeReview> existingReviews = reviewRepository.findByPerformanceCycleId(cycleId);
        Map<Long, EmployeeReview> reviewsByEmployeeId = existingReviews.stream()
                .collect(Collectors.toMap(review -> review.getEmployee().getId(), Function.identity()));
        List<EmployeeReview> newReviews = eligibleEmployees.stream()
                .filter(employee -> !reviewsByEmployeeId.containsKey(employee.getId()))
                .map(employee -> newReview(cycle, employee, publishedBy))
                .toList();
        reviewRepository.saveAll(newReviews);
        List<PerformanceCycleAssessor> assessorConfigs = assessorConfigRepository
                .findByPerformanceCycleIdOrderByDisplayOrderAsc(cycleId).stream()
                .filter(config -> Boolean.TRUE.equals(config.getActive())).toList();
        if (assessorConfigs.isEmpty()) {
            throw new IllegalStateException("Cycle must contain at least one active assessor");
        }
        List<EmployeeReview> eligibleReviews = eligibleEmployees.stream()
                .map(employee -> reviewsByEmployeeId.get(employee.getId()))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
        eligibleReviews.addAll(newReviews);
        List<EmployeeReviewAssessment> assessments = eligibleReviews.stream()
                .flatMap(review -> assessorConfigs.stream()
                        .filter(config -> !assessmentRepository.existsByEmployeeReviewIdAndAssessmentLevel(
                                review.getId(), config.getDisplayOrder()))
                        .map(config -> newAssessment(review, config, publishedBy))
                        .filter(java.util.Objects::nonNull))
                .toList();
        assessmentRepository.saveAll(assessments);

        eligibleReviews.forEach(review ->
                emailNotificationService.queueCyclePublished(cycle, review.getEmployee(), review));

        cycle.setStatus("PUBLISHED");
        cycle.setUpdatedBy(publishedBy);
        cycleRepository.save(cycle);

        return CyclePublishResponse.builder()
                .cycleId(cycleId)
                .cycleStatus(cycle.getStatus())
                .eligibleEmployees(eligibleEmployees.size())
                .reviewsCreated(newReviews.size())
                .reviewsSkipped(eligibleEmployees.size() - newReviews.size())
                .assessmentsCreated(assessments.size())
                .build();
    }

    private void validateConfiguration(PerformanceCycles cycle) {
        List<PerformanceCycleSection> sections =
                sectionRepository.findByPerformanceCycleIdOrderByDisplayOrderAsc(cycle.getId());
        if (sections.isEmpty()) {
            throw new IllegalStateException("Cycle must contain at least one assessment section");
        }
        boolean hasQuestions = sections.stream().anyMatch(section ->
                !questionRepository.findByPerformanceCycleSectionIdOrderByDisplayOrderAsc(section.getId()).isEmpty());
        if (!hasQuestions) {
            throw new IllegalStateException("Cycle must contain at least one assessment question");
        }
    }

    private List<Employee> determineEligibleEmployees(PerformanceCycles cycle) {
        LookupValue applicableType = lookupValueRepository.findById(cycle.getApplicableTypeId())
                .orElseThrow(() -> new IllegalStateException("Cycle has an invalid applicable type"));
        String code = applicableType.getCode().toUpperCase();
        List<Long> scopeIds = cycle.getScopeValueIds() == null ? List.of() : cycle.getScopeValueIds();

        return switch (code) {
            case "ALL" -> employeeRepository.findAll();
            case "DEPARTMENT" -> employeesForAssignments(
                    employeeAssignmentRepository.findByDepartmentIdInAndIsCurrentTrue(
                            requireScope(scopeIds, code)));
            case "DESIGNATION" -> employeesForAssignments(
                    employeeAssignmentRepository.findByDesignationIdInAndIsCurrentTrue(
                            requireScope(scopeIds, code)));
            case "EMPLOYEE" -> employeeRepository.findByIdIn(requireScope(scopeIds, code));
            default -> throw new IllegalStateException("Unsupported applicable type: " + code);
        };
    }

    private List<Employee> employeesForAssignments(List<EmployeeAssignment> assignments) {
        List<Long> employeeIds = assignments.stream()
                .map(EmployeeAssignment::getEmployeeId)
                .distinct()
                .toList();
        return employeeIds.isEmpty() ? List.of() : employeeRepository.findByIdIn(employeeIds);
    }

    private List<Long> requireScope(List<Long> scopeIds, String applicableType) {
        if (scopeIds.isEmpty()) {
            throw new IllegalStateException("scopeValueIds are required for " + applicableType);
        }
        return scopeIds;
    }

    private EmployeeReview newReview(PerformanceCycles cycle, Employee employee, Long publishedBy) {
        return EmployeeReview.builder()
                .performanceCycle(cycle)
                .employee(employee)
                .status(EmployeeReviewStatus.NOT_STARTED)
                .progressPercentage(BigDecimal.ZERO)
                .createdBy(publishedBy)
                .updatedBy(publishedBy)
                .build();
    }

    private EmployeeReviewAssessment newAssessment(EmployeeReview review, PerformanceCycleAssessor config,
            Long createdBy) {
        LookupValue role = lookupValueRepository.findById(config.getRoleId())
                .orElseThrow(() -> new IllegalStateException("Invalid assessor role: " + config.getRoleId()));
        if (!assigneeResolver.isApplicable(review.getEmployee(), role)) return null;
        Employee assessor = assigneeResolver.resolve(review.getEmployee(), role);
        return EmployeeReviewAssessment.builder().employeeReview(review)
                .assessmentLevel(config.getDisplayOrder()).assessorRole(role).assessorEmployee(assessor)
                .status(EmployeeReviewStatus.NOT_STARTED).progressPercentage(BigDecimal.ZERO)
                .createdBy(createdBy).updatedBy(createdBy).build();
    }

}
