package com.rit.performance.service;

import com.rit.performance.dto.CyclePublishResponse;
import com.rit.performance.dto.CyclePublishRequest;
import com.rit.performance.dto.CyclePublishCandidatesResponse;
import com.rit.performance.exception.InvalidOperationException;
import com.rit.performance.exception.ResourceNotFoundException;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.Comparator;
import com.rit.performance.entity.*;
import com.rit.performance.repository.EmployeeRepository;
import com.rit.performance.repository.EmployeeAssignmentRepository;
import com.rit.performance.repository.EmployeeReviewRepository;
import com.rit.performance.repository.EmployeeReviewAssessmentRepository;
import com.rit.performance.repository.PerformanceCycleAssessorRepository;
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
import java.util.HashMap;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
@AllArgsConstructor
public class ReviewCyclePublishServiceImpl extends WorkflowEventSource implements ReviewCyclePublishService {

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
    private final ApplicationEmailFactory emailFactory;
    private final UserInvitationService invitationService;


    @Override
    public CyclePublishResponse publish(Long cycleId, Long publishedBy, CyclePublishRequest request) {
        PerformanceCycles cycle = cycleRepository.findForPublication(cycleId)
                .orElseThrow(() -> new ResourceNotFoundException("Performance cycle not found: " + cycleId));

        validatePublishStatus(cycle);

        validateConfiguration(cycle);
        List<Employee> eligibleEmployees = determineEligibleEmployees(cycle);
        if (eligibleEmployees.isEmpty()) {
            throw new InvalidOperationException("No eligible employees found for this cycle");
        }

        List<Employee> selectedEmployees = selectEmployees(eligibleEmployees, request);
        if (selectedEmployees.isEmpty()) throw new InvalidOperationException("No employees selected for publication");
        List<PerformanceCycleAssessor> assessorConfigs = activeAssessors(cycleId);
        validateStages(assessorConfigs);
        List<EmployeeReview> existingReviews = reviewRepository.findForPublication(cycleId);
        Map<Long, EmployeeReview> reviewsByEmployeeId = existingReviews.stream()
                .collect(Collectors.toMap(review -> review.getEmployee().getId(), Function.identity()));
        int alreadySubmittedEmployees = (int) selectedEmployees.stream()
                .map(employee -> reviewsByEmployeeId.get(employee.getId()))
                .filter(java.util.Objects::nonNull)
                .filter(review -> review.getAssessments().stream().anyMatch(assessment ->
                        assessment.getStatus() == EmployeeReviewStatus.SUBMITTED
                                && "SELF".equals(AssessmentAssigneeResolver.stageCode(assessment.getAssessorRole()))))
                .count();
        // Validate every new enrollee before creating any records.
        selectedEmployees.stream().filter(employee -> !reviewsByEmployeeId.containsKey(employee.getId()))
                .forEach(assigneeResolver::reportingChain);
        // Include existing selections so HR can recover a missed setup invitation on republish.
        // Stable account lock order also serializes invitation checks across concurrent cycles.
        Map<Long, ApplicationEmail> setupEmails = new HashMap<>();
        for (Employee employee : selectedEmployees.stream().sorted(Comparator.comparing(Employee::getId)).toList()) {
            Optional<ApplicationEmail> setupEmail = invitationService.ensurePerformanceLogin(employee, cycle);
            if (setupEmail.isPresent()) {
                setupEmails.put(employee.getId(), setupEmail.get());
            }
        }
        List<EmployeeReview> newReviews = selectedEmployees.stream()
                .filter(employee -> !reviewsByEmployeeId.containsKey(employee.getId()))
                .map(employee -> newReview(cycle, employee, publishedBy))
                .toList();
        reviewRepository.saveAll(newReviews);
        List<EmployeeReviewAssessment> assessments = newReviews.stream()
                .flatMap(review -> assessorConfigs.stream()
                        .map(config -> newAssessment(review, config, publishedBy))
                        .filter(java.util.Objects::nonNull))
                .toList();
        assessmentRepository.saveAll(assessments);

        cycle.setStatus("PUBLISHED");
        if (cycle.getPublishedDate() == null) {
            cycle.setPublishedDate(LocalDateTime.now(ZoneOffset.UTC));
        }
        cycle.setUpdatedBy(publishedBy);
        cycleRepository.save(cycle);

        // Each employee's bell notification is saved asynchronously after commit.
        newReviews.forEach(review -> publishWorkflow(new NotificationEvents.ReviewAlert(
                review.getEmployee().getId(), review.getId(), "CYCLE_PUBLISHED",
                "Performance review assigned",
                cycle.getCycleName() + " is available. Open your performance review to complete your assessment.",
                cycle.getId() + ":" + review.getEmployee().getId())));

        // One background task sends the batch only after publication commits.
        List<ApplicationEmail> publicationEmails = new ArrayList<>();
        for (Employee employee : selectedEmployees) {
            ApplicationEmail setupEmail = setupEmails.get(employee.getId());
            if (setupEmail != null) {
                publicationEmails.add(setupEmail);
            } else if (!reviewsByEmployeeId.containsKey(employee.getId())
                    && employee.getEmail() != null && !employee.getEmail().isBlank()) {
                publicationEmails.add(emailFactory.cyclePublished(cycle, employee));
            }
        }
        if (!publicationEmails.isEmpty()) {
            publishWorkflow(new PerformancePublicationEmails(publicationEmails));
        }

        return CyclePublishResponse.builder()
                .cycleId(cycleId)
                .cycleStatus(cycle.getStatus())
                .eligibleEmployees(eligibleEmployees.size())
                .selectedEmployees(selectedEmployees.size())
                .excludedEmployees(eligibleEmployees.size() - selectedEmployees.size())
                .alreadySubmittedEmployees(alreadySubmittedEmployees)
                .emailsScheduled(publicationEmails.size())
                .bellNotificationsScheduled(newReviews.size())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CyclePublishCandidatesResponse getPublishCandidates(Long cycleId) {
        PerformanceCycles cycle = cycleRepository.findById(cycleId)
                .orElseThrow(() -> new ResourceNotFoundException("Performance cycle not found: " + cycleId));
        List<String> configurationWarnings = new ArrayList<>();
        try {
            validatePublishStatus(cycle);
        } catch (IllegalStateException | InvalidOperationException exception) {
            configurationWarnings.add(exception.getMessage());
        }
        try {
            validateConfiguration(cycle);
            validateStages(activeAssessors(cycleId));
        } catch (IllegalStateException | InvalidOperationException exception) {
            configurationWarnings.add(exception.getMessage());
        }
        List<Employee> eligible = List.of();
        try {
            eligible = determineEligibleEmployees(cycle);
        } catch (IllegalStateException | InvalidOperationException exception) {
            configurationWarnings.add(exception.getMessage());
        }
        Map<Long, EmployeeReview> reviews = reviewRepository.findByPerformanceCycleId(cycleId).stream()
                .collect(Collectors.toMap(review -> review.getEmployee().getId(), Function.identity()));
        List<CyclePublishCandidatesResponse.Candidate> candidates = eligible.stream()
                .map(employee -> candidate(employee, reviews.get(employee.getId()))).toList();
        int enrolled = (int) candidates.stream().filter(CyclePublishCandidatesResponse.Candidate::enrolled).count();
        int ready = (int) candidates.stream().filter(candidate -> !candidate.enrolled() && candidate.canPublish()).count();
        int warnings = (int) candidates.stream().filter(candidate -> !candidate.reportingChainWarnings().isEmpty()).count();
        int blocked = (int) candidates.stream().filter(candidate -> !candidate.canPublish()).count();
        return new CyclePublishCandidatesResponse(cycleId, cycle.getStatus(),
                configurationWarnings.isEmpty() && !candidates.isEmpty() && blocked == 0,
                configurationWarnings,
                new CyclePublishCandidatesResponse.Counts(candidates.size(), enrolled,
                        candidates.size() - enrolled, ready, warnings, blocked), candidates);
    }

    private CyclePublishCandidatesResponse.Candidate candidate(Employee employee, EmployeeReview review) {
        List<String> warnings = new ArrayList<>();
        List<Employee> chain = List.of();
        boolean valid = true;
        try {
            chain = assigneeResolver.reportingChain(employee);
            if (chain.isEmpty()) warnings.add("No reporting manager: only self assessment will be assigned; final publication requires a manager assessment");
            else if (chain.size() == 1) warnings.add("Reporting Manager 2 is unavailable: the reporting chain ends at Manager 1");
        } catch (InvalidOperationException exception) {
            warnings.add(exception.getMessage());
            valid = false;
        }
        Employee manager1 = chain.isEmpty() ? null : chain.get(0);
        Employee manager2 = chain.size() < 2 ? null : chain.get(1);
        return new CyclePublishCandidatesResponse.Candidate(employee.getId(), employeeName(employee),
                employee.getRitId(), employee.getEmail(), review != null,
                review == null ? null : review.getId(), review == null ? null : review.getStatus(),
                manager1 == null ? null : manager1.getId(), manager1 == null ? null : employeeName(manager1),
                manager2 == null ? null : manager2.getId(), manager2 == null ? null : employeeName(manager2),
                review != null || valid, warnings);
    }

    private List<PerformanceCycleAssessor> activeAssessors(Long cycleId) {
        return assessorConfigRepository.findByPerformanceCycleIdOrderByDisplayOrderAsc(cycleId).stream()
                .filter(config -> Boolean.TRUE.equals(config.getActive())).toList();
    }

    private List<Employee> selectEmployees(List<Employee> eligible, CyclePublishRequest request) {
        CyclePublishRequest selection = request == null ? new CyclePublishRequest() : request;
        if (selection.getMode() == null) throw new InvalidOperationException("mode is required");
        List<Long> ids = selection.getEmployeeList() == null ? List.of() : selection.getEmployeeList();
        Set<Long> uniqueIds = new HashSet<>(ids);
        if (ids.stream().anyMatch(id -> id == null || id <= 0) || uniqueIds.size() != ids.size())
            throw new InvalidOperationException("employeeIds must contain unique positive employee IDs");
        if (selection.getMode() == CyclePublishRequest.Mode.ALL && !ids.isEmpty())
            throw new InvalidOperationException("employeeIds must be empty for ALL mode");
        if (selection.getMode() == CyclePublishRequest.Mode.SELECTED && ids.isEmpty())
            throw new InvalidOperationException("employeeIds must be nonempty for SELECTED mode");
        Set<Long> eligibleIds = eligible.stream().map(Employee::getId).collect(Collectors.toSet());
        List<Long> invalid = ids.stream().filter(id -> !eligibleIds.contains(id)).toList();
        if (!invalid.isEmpty())
            throw new InvalidOperationException("Employees are not active and eligible for this cycle: " + invalid);
        return switch (selection.getMode()) {
            case ALL -> eligible;
            case SELECTED -> eligible.stream().filter(employee -> uniqueIds.contains(employee.getId())).toList();
            case EXCLUDE -> eligible.stream().filter(employee -> !uniqueIds.contains(employee.getId())).toList();
        };
    }

    private void validatePublishStatus(PerformanceCycles cycle) {
        if (!Set.of("DRAFT", "ACTIVE", "PUBLISHED").contains(cycle.getStatus().toUpperCase(java.util.Locale.ROOT)))
            throw new InvalidOperationException("Only DRAFT, ACTIVE, or PUBLISHED cycles can be published");
    }

    private String employeeName(Employee employee) {
        return (employee.getFirstName() + " " + (employee.getLastName() == null ? "" : employee.getLastName())).trim();
    }

    private void validateStages(List<PerformanceCycleAssessor> configs) {
        java.util.Set<String> stages = new java.util.HashSet<>();
        java.util.Set<Integer> levels = new java.util.HashSet<>();
        int previousOrder = -1;
        for (PerformanceCycleAssessor config : configs) {
            LookupValue role = lookupValueRepository.findById(config.getRoleId())
                    .orElseThrow(() -> new InvalidOperationException("Invalid performance stage: " + config.getRoleId()));
            String stage = AssessmentAssigneeResolver.stageCode(role);
            int order = switch (stage) {
                case "SELF" -> 0;
                case "REPORTING_MANAGER_1" -> 1;
                case "REPORTING_MANAGER_2" -> 2;
                default -> 3;
            };
            if (!role.isActive() || !stages.add(stage) || !levels.add(config.getDisplayOrder()) || order <= previousOrder)
                throw new InvalidOperationException("Performance stages must be unique and ordered Self, Reporting Manager 1, Reporting Manager 2, HR");
            previousOrder = order;
        }
        if (!stages.contains("SELF") || !stages.contains("REPORTING_MANAGER_1"))
            throw new InvalidOperationException("Cycle must contain Self and Reporting Manager 1 stages");
    }

    private void validateConfiguration(PerformanceCycles cycle) {
        List<PerformanceCycleSection> sections = sectionRepository.findByPerformanceCycleIdOrderByDisplayOrderAsc(cycle.getId());
        if (sections.isEmpty()) {
            throw new InvalidOperationException("Cycle must contain at least one assessment section");
        }
        boolean hasQuestions = sections.stream().anyMatch(section ->
                !questionRepository.findByPerformanceCycleSectionIdOrderByDisplayOrderAsc(section.getId()).isEmpty());
        if (!hasQuestions) {
            throw new InvalidOperationException("Cycle must contain at least one assessment question");
        }
    }

    private List<Employee> determineEligibleEmployees(PerformanceCycles cycle) {
        LookupValue applicableType = lookupValueRepository.findById(cycle.getApplicableTypeId())
                .orElseThrow(() -> new InvalidOperationException("Cycle has an invalid applicable type"));
        String code = applicableType.getCode().toUpperCase();
        List<Long> scopeIds = cycle.getScopeValueIds() == null ? List.of() : cycle.getScopeValueIds();

        List<Employee> employees = switch (code) {
            case "ALL" -> employeeRepository.findAll();
            case "DEPARTMENT" -> employeesForAssignments(
                    employeeAssignmentRepository.findByDepartmentIdInAndStatusIgnoreCase(
                            requireScope(scopeIds, code), "ACTIVE"));
            case "DESIGNATION" -> employeeRepository.findByDesignationIdInAndStatusIgnoreCase(
                    requireScope(scopeIds, code), "ACTIVE");
            case "EMPLOYEE" -> employeeRepository.findByIdIn(requireScope(scopeIds, code));
            default -> throw new InvalidOperationException("Unsupported applicable type: " + code);
        };
        return employees.stream().filter(employee -> "ACTIVE".equalsIgnoreCase(employee.getStatus()))
                .sorted(Comparator.comparing(this::employeeName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(Employee::getId)).toList();
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
            throw new InvalidOperationException("scopeValueIds are required for " + applicableType);
        }
        return scopeIds;
    }

    private EmployeeReview newReview(PerformanceCycles cycle, Employee employee, Long publishedBy) {
        EmployeeReview review = EmployeeReview.builder()
                .performanceCycle(cycle)
                .employee(employee)
                .status(EmployeeReviewStatus.NOT_STARTED)
                .progressPercentage(BigDecimal.ZERO)
                .createdBy(publishedBy)
                .updatedBy(publishedBy)
                .build();
        employeeAssignmentRepository.findActiveByEmployeeId(employee.getId())
                .ifPresent(assignment -> applyAssignmentSnapshot(review, assignment));
        return review;
    }

    private void applyAssignmentSnapshot(EmployeeReview review, EmployeeAssignment assignment) {
        review.setSowId(assignment.getSowId());
    }

    private EmployeeReviewAssessment newAssessment(EmployeeReview review, PerformanceCycleAssessor config,
            Long createdBy) {
        LookupValue role = lookupValueRepository.findById(config.getRoleId())
                .orElseThrow(() -> new InvalidOperationException("Invalid assessor role: " + config.getRoleId()));
        if (assigneeResolver.isPublishOnly(role)) return null;
        Employee assessor = assigneeResolver.resolveIfAvailable(review.getEmployee(), role).orElse(null);
        if (assessor == null) return null;
        return EmployeeReviewAssessment.builder().employeeReview(review)
                .assessmentLevel(config.getDisplayOrder()).assessorRole(role).assessorEmployee(assessor)
                .status(EmployeeReviewStatus.NOT_STARTED).progressPercentage(BigDecimal.ZERO)
                .createdBy(createdBy).updatedBy(createdBy).build();
    }

}
