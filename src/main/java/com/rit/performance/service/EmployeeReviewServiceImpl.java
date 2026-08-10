package com.rit.performance.service;

import com.rit.performance.dto.*;
import com.rit.performance.entity.*;
import com.rit.performance.exception.InvalidOperationException;
import com.rit.performance.exception.ResourceNotFoundException;
import com.rit.performance.mapper.EmployeeReviewMapper;
import com.rit.performance.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class EmployeeReviewServiceImpl implements EmployeeReviewService {
    private final EmployeeReviewRepository reviewRepository;
    private final EmployeeReviewAssessmentRepository assessmentRepository;
    private final EmployeeReviewAnswerRepository answerRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeAssignmentRepository employeeAssignmentRepository;
    private final ProjectRepository projectRepository;
    private final FinalRatingRepository finalRatingRepository;
    private final PerformanceCycleTimelineRepository timelineRepository;
    private final PerformanceCycleQuestionRepository questionRepository;
    private final PerformanceCycleSectionRepository sectionRepository;
    private final LookupValueRepository lookupValueRepository;
    private final PerformanceCycleAssessorRepository assessorConfigRepository;
    private final AssessmentAssigneeResolver assigneeResolver;
    private final PerformanceCycleConfigRepository cycleRepository;
    private final EmailNotificationService emailNotificationService;

    public EmployeeReviewServiceImpl(EmployeeReviewRepository reviewRepository,
            EmployeeReviewAssessmentRepository assessmentRepository,
            EmployeeReviewAnswerRepository answerRepository, EmployeeRepository employeeRepository,
            EmployeeAssignmentRepository employeeAssignmentRepository, ProjectRepository projectRepository,
            FinalRatingRepository finalRatingRepository, PerformanceCycleTimelineRepository timelineRepository,
            PerformanceCycleConfigRepository cycleRepository,
            PerformanceCycleQuestionRepository questionRepository,
            PerformanceCycleSectionRepository sectionRepository,
            LookupValueRepository lookupValueRepository,
            PerformanceCycleAssessorRepository assessorConfigRepository,
            AssessmentAssigneeResolver assigneeResolver,
            EmailNotificationService emailNotificationService) {
        this.reviewRepository = reviewRepository; this.assessmentRepository = assessmentRepository;
        this.answerRepository = answerRepository; this.employeeRepository = employeeRepository;
        this.employeeAssignmentRepository = employeeAssignmentRepository;
        this.projectRepository = projectRepository; this.finalRatingRepository = finalRatingRepository;
        this.timelineRepository = timelineRepository;
        this.questionRepository = questionRepository; this.sectionRepository = sectionRepository;
        this.lookupValueRepository = lookupValueRepository;
        this.assessorConfigRepository = assessorConfigRepository; this.assigneeResolver = assigneeResolver;
        this.cycleRepository = cycleRepository;
        this.emailNotificationService = emailNotificationService;
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewProgressResponse getReviewProgress(Long cycleId) {
        PerformanceCycles cycle = cycleRepository.findById(cycleId)
                .orElseThrow(() -> new ResourceNotFoundException("Performance cycle not found: " + cycleId));
        List<EmployeeReview> reviews = reviewRepository.findByPerformanceCycleId(cycleId);
        List<Long> reviewIds = reviews.stream().map(EmployeeReview::getId).toList();
        Map<Long, FinalRating> ratingsByReview = reviewIds.isEmpty() ? Map.of()
                : finalRatingRepository.findByEmployeeReviewIdIn(reviewIds).stream()
                    .collect(Collectors.toMap(rating -> rating.getEmployeeReview().getId(), Function.identity()));
        Map<Long, Employee> employees = employeeRepository.findAll().stream()
                .collect(Collectors.toMap(Employee::getId, Function.identity()));
        Map<Long, LookupValue> lookups = lookupValueRepository.findAll().stream()
                .collect(Collectors.toMap(LookupValue::getId, Function.identity()));
        Map<Long, Projects> projects = projectRepository.findAll().stream()
                .collect(Collectors.toMap(Projects::getId, Function.identity()));
        List<PerformanceCycleTimeline> timelines = timelineRepository
                .findByPerformanceCycleIdOrderByDisplayOrderAsc(cycleId);
        Map<Long, LocalDate> dueDatesByRole = timelines.stream()
                .filter(timeline -> timeline.getRoleId() != null)
                .collect(Collectors.toMap(PerformanceCycleTimeline::getRoleId,
                        PerformanceCycleTimeline::getEndDate, (first, ignored) -> first));
        Map<Integer, LocalDate> dueDatesByLevel = timelines.stream()
                .collect(Collectors.toMap(PerformanceCycleTimeline::getDisplayOrder,
                        PerformanceCycleTimeline::getEndDate, (first, ignored) -> first));

        List<ReviewProgressEmployeeResponse> employeeRows = reviews.stream()
                .sorted(java.util.Comparator.comparing(review -> employeeName(review.getEmployee()),
                        String.CASE_INSENSITIVE_ORDER))
                .map(review -> toProgressEmployee(review, legacyAssignment(review), employees, lookups, projects,
                        ratingsByReview.get(review.getId()),
                        dueDatesByRole, dueDatesByLevel))
                .toList();
        ReviewProgressSummaryResponse summary = ReviewProgressSummaryResponse.builder()
                .totalEmployees(reviews.size())
                .selfReviewsCompleted(countSubmitted(reviews, "EMPLOYEE"))
                .teamLeadReviewsCompleted(countSubmitted(reviews, "TEAM_LEAD"))
                .managerReviewsCompleted(countSubmitted(reviews, "MANAGER"))
                .ratingsPublished(ratingsByReview.values().stream()
                        .filter(rating -> Boolean.TRUE.equals(rating.getPublished())).count())
                .build();
        java.time.LocalDateTime publishedDate = ratingsByReview.values().stream()
                .filter(rating -> Boolean.TRUE.equals(rating.getPublished()))
                .map(FinalRating::getPublishedDate)
                .filter(java.util.Objects::nonNull)
                .max(java.time.LocalDateTime::compareTo)
                .orElse(null);
        return ReviewProgressResponse.builder().cycleId(cycleId).cycleName(cycle.getCycleName())
                .cycleStatus(cycle.getStatus()).publishedDate(publishedDate)
                .summary(summary).employees(employeeRows).build();
    }

    private long countSubmitted(List<EmployeeReview> reviews, String roleType) {
        return reviews.stream().filter(review -> assessmentByRole(review, roleType) != null)
                .filter(review -> assessmentByRole(review, roleType).getStatus() == EmployeeReviewStatus.SUBMITTED)
                .count();
    }

    private ReviewProgressEmployeeResponse toProgressEmployee(EmployeeReview review,
            EmployeeAssignment legacyAssignment, Map<Long, Employee> employees, Map<Long, LookupValue> lookups,
            Map<Long, Projects> projects, FinalRating rating, Map<Long, LocalDate> dueDatesByRole,
            Map<Integer, LocalDate> dueDatesByLevel) {
        Employee employee = review.getEmployee();
        Long departmentId = legacyAssignment == null ? null : legacyAssignment.getDepartmentId();
        Long designationId = legacyAssignment == null ? null : legacyAssignment.getDesignationId();
        Long projectId = snapshotOrLegacy(review.getProjectSnapshotId(),
                legacyAssignment == null ? null : legacyAssignment.getProjectId());
        Long managerId = legacyAssignment == null ? null : legacyAssignment.getManagerId();
        Long leadId = legacyAssignment == null ? null : legacyAssignment.getLeadId();
        LookupValue department = lookups.get(departmentId);
        LookupValue designation = lookups.get(designationId);
        Projects project = projects.get(projectId);
        Employee manager = employees.get(managerId);
        Employee lead = employees.get(leadId);
        EmployeeReviewAssessment self = assessmentByRole(review, "EMPLOYEE");
        EmployeeReviewAssessment teamLead = assessmentByRole(review, "TEAM_LEAD");
        EmployeeReviewAssessment managerAssessment = assessmentByRole(review, "MANAGER");
        boolean published = rating != null && Boolean.TRUE.equals(rating.getPublished());
        EmployeeReviewAssessment pending = pendingAssessment(review, published);
        return ReviewProgressEmployeeResponse.builder()
                .employeeId(employee.getId())
                .employeeName(employeeName(employee))
                .designationName(designation == null ? null : designation.getName())
                .departmentName(department == null ? null : department.getName())
                .projectName(project == null ? null : project.getProjectName())
                .managerId(managerId)
                .managerName(manager == null ? null : employeeName(manager))
                .leadId(leadId)
                .leadName(lead == null ? null : employeeName(lead))
                .reviewId(review.getId()).currentStage(currentStage(review, published))
                .pendingWithEmployeeId(pending == null || pending.getAssessorEmployee() == null
                        ? null : pending.getAssessorEmployee().getId())
                .pendingWithEmployeeName(pending == null || pending.getAssessorEmployee() == null
                        ? null : employeeName(pending.getAssessorEmployee()))
                .pendingWithRole(pending == null ? null : normalizedAssessmentRole(pending))
                .lastUpdatedAt(lastUpdatedAt(review, rating))
                .reviewStatus(review.getStatus()).progressPercentage(review.getProgressPercentage())
                .selfAssessmentStatus(self == null ? null : self.getStatus())
                .teamLeadAssessmentStatus(teamLead == null ? null : teamLead.getStatus())
                .managerAssessmentStatus(managerAssessment == null ? null : managerAssessment.getStatus())
                .assessments(review.getAssessments().stream()
                        .sorted(java.util.Comparator.comparing(EmployeeReviewAssessment::getAssessmentLevel))
                        .map(assessment -> {
                            Long roleId = assessment.getAssessorRole() == null
                                    ? null : assessment.getAssessorRole().getId();
                            LocalDate dueDate = assessment.getDueDate();
                            if (dueDate == null) dueDate = roleId == null ? null : dueDatesByRole.get(roleId);
                            if (dueDate == null) dueDate = dueDatesByLevel.get(assessment.getAssessmentLevel());
                            return ReviewProgressAssessmentResponse.builder()
                                    .stageId(assessment.getId())
                                    .roleName(assessment.getAssessorRole() == null
                                            ? null : assessment.getAssessorRole().getName())
                                    .status(assessment.getStatus())
                                    .dueDate(dueDate)
                                    .overdue(pending != null && java.util.Objects.equals(
                                            assessment.getId(), pending.getId())
                                            && calculateOverdue(dueDate, assessment.getStatus(), LocalDate.now()))
                                    .build();
                        })
                        .toList())
                .ratingPublished(published).build();
    }

    static boolean calculateOverdue(LocalDate dueDate, EmployeeReviewStatus status, LocalDate today) {
        return dueDate != null && today.isAfter(dueDate) && status != EmployeeReviewStatus.SUBMITTED;
    }

    @Override
    public ReviewProgressResponse reopenAssessments(Long cycleId, ReopenAssessmentsRequest request) {
        if (!cycleRepository.existsById(cycleId)) {
            throw new ResourceNotFoundException("Performance cycle not found: " + cycleId);
        }

        Set<Long> employeeIds = new LinkedHashSet<>(request.getEmployeeIds());
        Map<Long, EmployeeReview> reviewsByEmployee = reviewRepository
                .findByPerformanceCycleIdAndEmployeeIdIn(cycleId, new ArrayList<>(employeeIds)).stream()
                .collect(Collectors.toMap(review -> review.getEmployee().getId(), Function.identity()));

        List<PerformanceCycleTimeline> timelines = timelineRepository.findByPerformanceCycleIdOrderByDisplayOrderAsc(cycleId);
        LocalDate today = LocalDate.now();
        List<EmployeeReviewAssessment> assessments = new ArrayList<>();
        for (Long employeeId : employeeIds) {
            EmployeeReview review = reviewsByEmployee.get(employeeId);
            if (review == null) {
                throw new ResourceNotFoundException("Employee review not found for employee "
                        + employeeId + " in cycle " + cycleId);
            }
            EmployeeReviewAssessment assessment = pendingAssessment(review, false);
            if (assessment == null) {
                throw new InvalidOperationException("Employee " + employeeId
                        + " has no incomplete assessment in cycle " + cycleId);
            }
            LocalDate currentDueDate = effectiveDueDate(assessment, timelines);
            if (!calculateOverdue(currentDueDate, assessment.getStatus(), today)) {
                throw new InvalidOperationException("Current assessment for employee " + employeeId
                        + " is not overdue and cannot be reopened");
            }
            assessments.add(assessment);
        }

        String reason = request.getReason().trim();
        LocalDateTime reopenedDate = LocalDateTime.now();
        LocalDate newDueDate = today.plusDays(request.getDaysPerStage());
        assessments.forEach(assessment -> {
            EmployeeReview review = assessment.getEmployeeReview();
            review.setExtensionDaysPerStage(request.getDaysPerStage());
            review.setExtensionReason(reason);
            review.setExtensionGrantedDate(reopenedDate);
            assessment.setDueDate(newDueDate);
            assessment.setReopenReason(reason);
            assessment.setReopenedDate(reopenedDate);
        });
        reviewRepository.saveAll(assessments.stream()
                .map(EmployeeReviewAssessment::getEmployeeReview).toList());
        assessmentRepository.saveAll(assessments);

        if (request.isNotifyAssignees()) assessments.forEach(assessment ->
                emailNotificationService.queueAssessmentReopened(assessment.getEmployeeReview(), assessment,
                        newDueDate, reason));
        return getReviewProgress(cycleId);
    }

    private LocalDate effectiveDueDate(
            EmployeeReviewAssessment assessment, List<PerformanceCycleTimeline> timelines) {
        if (assessment.getDueDate() != null) return assessment.getDueDate();
        return timelineDueDate(assessment, timelines);
    }

    private LocalDate timelineDueDate(
            EmployeeReviewAssessment assessment, List<PerformanceCycleTimeline> timelines) {
        return timelines.stream()
                .filter(timeline -> assessment.getAssessorRole() != null
                        && timeline.getRoleId() != null
                        && timeline.getRoleId().equals(assessment.getAssessorRole().getId()))
                .findFirst()
                .or(() -> timelines.stream()
                        .filter(timeline -> timeline.getDisplayOrder()
                                .equals(assessment.getAssessmentLevel()))
                        .findFirst())
                .map(PerformanceCycleTimeline::getEndDate)
                .orElse(null);
    }

    private EmployeeReviewAssessment assessmentByRole(EmployeeReview review, String roleType) {
        return review.getAssessments().stream()
                .filter(assessment -> roleType.equals(normalizedAssessmentRole(assessment)))
                .findFirst().orElse(null);
    }

    private String normalizedAssessmentRole(EmployeeReviewAssessment assessment) {
        if (isSelfAssessment(assessment, assessment.getEmployeeReview().getEmployee().getId())) return "EMPLOYEE";
        if (assessment.getAssessorRole() == null) return "UNKNOWN";
        String value = assessment.getAssessorRole().getCode() == null
                || assessment.getAssessorRole().getCode().isBlank()
                ? assessment.getAssessorRole().getName() : assessment.getAssessorRole().getCode();
        String normalized = value.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        if (normalized.equals("TL") || normalized.equals("LEAD") || normalized.equals("TEAMLEAD"))
            return "TEAM_LEAD";
        return normalized;
    }

    private String currentStage(EmployeeReview review, boolean published) {
        if (published) return "PUBLISHED";
        return java.util.Optional.ofNullable(pendingAssessment(review, false))
                .map(this::normalizedAssessmentRole)
                .map(role -> role + "_REVIEW")
                .orElse(review.getStatus() == EmployeeReviewStatus.SUBMITTED ? "COMPLETED" : "NOT_STARTED");
    }

    private EmployeeReviewAssessment pendingAssessment(EmployeeReview review, boolean published) {
        if (published) return null;
        return review.getAssessments().stream()
                .filter(assessment -> assessment.getStatus() != EmployeeReviewStatus.SUBMITTED)
                .min(java.util.Comparator.comparing(EmployeeReviewAssessment::getAssessmentLevel))
                .orElse(null);
    }

    private java.time.LocalDateTime lastUpdatedAt(EmployeeReview review, FinalRating rating) {
        java.util.stream.Stream<java.time.LocalDateTime> reviewDates = java.util.stream.Stream.of(
                review.getCreatedDate(), review.getUpdatedDate(), rating == null ? null : rating.getPublishedDate());
        java.util.stream.Stream<java.time.LocalDateTime> assessmentDates = review.getAssessments().stream()
                .flatMap(assessment -> java.util.stream.Stream.of(
                        assessment.getCreatedDate(), assessment.getUpdatedDate(),
                        assessment.getStartedDate(), assessment.getSubmittedDate()));
        return java.util.stream.Stream.concat(reviewDates, assessmentDates)
                .filter(java.util.Objects::nonNull)
                .max(java.time.LocalDateTime::compareTo)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public TeamReviewDashboardResponse getTeamReviews(Long assessorEmployeeId, Long cycleId) {
        Employee assessor = employeeRepository.findById(assessorEmployeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Assessor employee not found: " + assessorEmployeeId));
        PerformanceCycles cycle = cycleRepository.findById(cycleId)
                .orElseThrow(() -> new ResourceNotFoundException("Performance cycle not found: " + cycleId));
        List<EmployeeReviewAssessment> assignedReviews = assessmentRepository
                .findAssignedReviewsForAssessor(assessorEmployeeId, cycleId);
        List<Long> reportIds = assignedReviews.stream()
                .map(assessment -> assessment.getEmployeeReview().getEmployee().getId())
                .distinct().toList();
        List<Employee> reports = reportIds.isEmpty() ? List.of() : employeeRepository.findByIdIn(reportIds).stream()
                .filter(employee -> "ACTIVE".equalsIgnoreCase(employee.getStatus()))
                .sorted(java.util.Comparator.comparing(this::employeeName, String.CASE_INSENSITIVE_ORDER)).toList();

        Map<Long, EmployeeReview> reviewsByEmployee = reportIds.isEmpty() ? Map.of()
                : reviewRepository.findByPerformanceCycleIdAndEmployeeIdIn(cycleId, reportIds).stream()
                    .collect(Collectors.toMap(review -> review.getEmployee().getId(), Function.identity()));
        List<Long> reviewIds = reviewsByEmployee.values().stream().map(EmployeeReview::getId).toList();
        Map<Long, List<EmployeeReviewAssessment>> assessmentsByReview = reviewIds.isEmpty() ? Map.of()
                : assessmentRepository.findByEmployeeReviewIdInOrderByEmployeeReviewIdAscAssessmentLevelAsc(reviewIds)
                    .stream().collect(Collectors.groupingBy(a -> a.getEmployeeReview().getId()));

        List<TeamReviewMemberResponse> members = reports.stream().map(employee -> toTeamMember(employee,
                reviewsByEmployee.get(employee.getId()), assessmentsByReview, assessorEmployeeId)).toList();
        return TeamReviewDashboardResponse.builder().cycleId(cycleId).cycleName(cycle.getCycleName())
                .assessorEmployeeId(assessorEmployeeId).assessorEmployeeName(employeeName(assessor))
                .teamMembers(members).build();
    }

    private TeamReviewMemberResponse toTeamMember(Employee employee, EmployeeReview review,
            Map<Long, List<EmployeeReviewAssessment>> assessmentsByReview, Long assessorEmployeeId) {
        if (review == null) return baseTeamMember(employee).workflowState("NOT_ENROLLED").actionRequired(false).build();
        List<EmployeeReviewAssessment> assessments = assessmentsByReview.getOrDefault(review.getId(), List.of());
        EmployeeReviewAssessment self = assessments.stream().filter(a -> isSelfAssessment(a, employee.getId()))
                .findFirst().orElse(null);
        EmployeeReviewAssessment assigned = assessments.stream()
                .filter(a -> a.getAssessorEmployee() != null
                        && assessorEmployeeId.equals(a.getAssessorEmployee().getId())
                        && !isSelfAssessment(a, employee.getId()))
                .findFirst().orElse(null);
        boolean previousLevelPending = assigned != null && assessments.stream()
                .filter(a -> a.getAssessmentLevel() < assigned.getAssessmentLevel())
                .anyMatch(a -> a.getStatus() != EmployeeReviewStatus.SUBMITTED);
        String state;
        boolean actionRequired = false;
        if (self == null || self.getStatus() == EmployeeReviewStatus.NOT_STARTED) state = "SELF_NOT_STARTED";
        else if (self.getStatus() == EmployeeReviewStatus.IN_PROGRESS) state = "SELF_IN_PROGRESS";
        else if (previousLevelPending) state = "AWAITING_PREVIOUS_REVIEW";
        else if (assigned != null && assigned.getStatus() == EmployeeReviewStatus.NOT_STARTED) {
            state = "PENDING_MY_REVIEW"; actionRequired = true;
        } else if (assigned != null && assigned.getStatus() == EmployeeReviewStatus.IN_PROGRESS) {
            state = "MY_REVIEW_IN_PROGRESS"; actionRequired = true;
        } else if (assigned != null && assigned.getStatus() == EmployeeReviewStatus.SUBMITTED) {
            state = "MY_REVIEW_COMPLETED";
        } else if (assessments.stream().anyMatch(a -> !isSelfAssessment(a, employee.getId())
                && a.getStatus() != EmployeeReviewStatus.SUBMITTED)) state = "AWAITING_PREVIOUS_REVIEW";
        else state = "AWAITING_ASSIGNMENT";

        TeamReviewMemberResponse.TeamReviewMemberResponseBuilder builder = baseTeamMember(employee)
                .reviewId(review.getId()).reviewStatus(review.getStatus()).workflowState(state)
                .actionRequired(actionRequired);
        if (self != null) builder.selfAssessmentId(self.getId()).selfAssessmentStatus(self.getStatus());
        if (assigned != null) builder.assignedAssessmentId(assigned.getId())
                .assignedAssessmentLevel(assigned.getAssessmentLevel())
                .assignedRoleId(assigned.getAssessorRole() == null ? null : assigned.getAssessorRole().getId())
                .assignedRoleName(assigned.getAssessorRole() == null ? null : assigned.getAssessorRole().getName())
                .assignedAssessmentStatus(assigned.getStatus())
                .assignedProgressPercentage(assigned.getStatus() == EmployeeReviewStatus.SUBMITTED
                        ? new BigDecimal("100.00") : assigned.getProgressPercentage());
        return builder.build();
    }

    private TeamReviewMemberResponse.TeamReviewMemberResponseBuilder baseTeamMember(Employee employee) {
        return TeamReviewMemberResponse.builder().employeeId(employee.getId())
                .employeeName(employeeName(employee));
    }

    private boolean isSelfAssessment(EmployeeReviewAssessment assessment, Long employeeId) {
        if (assessment.getAssessorRole() != null && "EMPLOYEE".equalsIgnoreCase(assessment.getAssessorRole().getCode()))
            return true;
        return assessment.getAssessorEmployee() != null
                && employeeId.equals(assessment.getAssessorEmployee().getId())
                && assessment.getAssessmentLevel() != null && assessment.getAssessmentLevel() == 1;
    }

    private String employeeName(Employee employee) {
        return (employee.getFirstName() + " " + (employee.getLastName() == null ? "" : employee.getLastName())).trim();
    }

    private Long snapshotOrLegacy(Long snapshotValue, Long legacyValue) {
        return snapshotValue == null ? legacyValue : snapshotValue;
    }

    private EmployeeAssignment legacyAssignment(EmployeeReview review) {
        LocalDate reviewDate = review.getCreatedDate() == null ? LocalDate.now()
                : review.getCreatedDate().toLocalDate();
        return employeeAssignmentRepository.findEffectiveOnDate(review.getEmployee().getId(), reviewDate).stream()
                .findFirst()
                .orElseGet(() -> employeeAssignmentRepository
                        .findByEmployeeIdAndIsCurrentTrue(review.getEmployee().getId()).orElse(null));
    }

    @Override @Transactional(readOnly = true)
    public EmployeeReviewResponse getReviewById(Long id) { return EmployeeReviewMapper.toResponse(findReview(id)); }

    @Override @Transactional(readOnly = true)
    public EmployeeReviewResponse getEmployeeReview(Long employeeId, Long cycleId, Long assessorId) {
        EmployeeReview review = findEmployeeReview(employeeId, cycleId);
        if (assessorId == null || employeeId.equals(assessorId))
            return EmployeeReviewMapper.toEmployeeResponse(review, employeeId);

        List<EmployeeReviewAssessment> assigned = review.getAssessments().stream()
                .filter(assessment -> assessment.getAssessorEmployee() != null
                        && assessorId.equals(assessment.getAssessorEmployee().getId()))
                .toList();
        if (assigned.isEmpty())
            throw new InvalidOperationException("Assessor is not assigned to this employee review");

        int visibleThroughLevel = assigned.stream()
                .map(EmployeeReviewAssessment::getAssessmentLevel)
                .filter(java.util.Objects::nonNull)
                .max(Integer::compareTo).orElse(0);
        List<EmployeeReviewAssessment> visible = review.getAssessments().stream()
                .filter(assessment -> isVisibleToAssessor(
                        assessment, assessorId, visibleThroughLevel))
                .sorted(java.util.Comparator.comparing(EmployeeReviewAssessment::getAssessmentLevel))
                .toList();
        return EmployeeReviewMapper.toResponse(review, visible);
    }

    private boolean isVisibleToAssessor(EmployeeReviewAssessment assessment, Long assessorId,
            int visibleThroughLevel) {
        if (assessment.getAssessmentLevel() == null) return false;
        if (assessment.getAssessmentLevel() < visibleThroughLevel)
            return assessment.getStatus() == EmployeeReviewStatus.SUBMITTED;
        return assessment.getAssessmentLevel() == visibleThroughLevel
                && assessment.getAssessorEmployee() != null
                && assessorId.equals(assessment.getAssessorEmployee().getId());
    }

    @Override @Transactional(readOnly = true)
    public List<EmployeeCycleReviewResponse> getEmployeeCycles(Long employeeId) {
        if (!employeeRepository.existsById(employeeId)) throw new ResourceNotFoundException("Employee not found");
        List<EmployeeReview> reviews = reviewRepository.findByEmployeeIdOrderByCreatedDateDesc(employeeId);
        List<Long> cycleIds = reviews.stream().map(review -> review.getPerformanceCycle().getId()).distinct().toList();
        Map<Long, List<PerformanceCycleTimeline>> timelinesByCycle = cycleIds.isEmpty() ? Map.of()
                : timelineRepository
                    .findByPerformanceCycleIdInOrderByPerformanceCycleIdAscDisplayOrderAsc(cycleIds).stream()
                    .collect(Collectors.groupingBy(PerformanceCycleTimeline::getPerformanceCycleId));
        return reviews.stream()
                .map(review -> toCycleResponse(review,
                        timelinesByCycle.getOrDefault(review.getPerformanceCycle().getId(), List.of())))
                .toList();
    }

    private EmployeeCycleReviewResponse toCycleResponse(
            EmployeeReview review, List<PerformanceCycleTimeline> timelines) {
        PerformanceCycles cycle = review.getPerformanceCycle();
        EmployeeReviewAssessment employeeAssessment = review.getAssessments().stream()
                .filter(assessment -> assessment.getAssessorEmployee() != null
                        && assessment.getAssessorEmployee().getId().equals(review.getEmployee().getId()))
                .min(java.util.Comparator.comparing(EmployeeReviewAssessment::getAssessmentLevel))
                .orElse(null);
        LocalDateTime startedDate = employeeAssessment == null ? null : employeeAssessment.getStartedDate();
        LocalDateTime submittedDate = employeeAssessment == null ? null : employeeAssessment.getSubmittedDate();
        BigDecimal employeeProgress = employeeAssessment == null ? BigDecimal.ZERO
                : employeeAssessment.getStatus() == EmployeeReviewStatus.SUBMITTED
                    ? new BigDecimal("100.00") : employeeAssessment.getProgressPercentage();
        LocalDate originalDueDate = employeeAssessment == null
                ? null : timelineDueDate(employeeAssessment, timelines);
        LocalDate dueDate = employeeAssessment == null ? null : effectiveDueDate(employeeAssessment, timelines);
        boolean extended = employeeAssessment != null && employeeAssessment.getReopenedDate() != null;
        LocalDateTime reviewCreatedDate = review.getCreatedDate() != null ? review.getCreatedDate()
                : review.getAssessments().stream().map(EmployeeReviewAssessment::getCreatedDate)
                    .filter(java.util.Objects::nonNull).min(LocalDateTime::compareTo).orElse(null);
        LocalDateTime reviewUpdatedDate = review.getUpdatedDate() != null ? review.getUpdatedDate()
                : review.getAssessments().stream().map(EmployeeReviewAssessment::getUpdatedDate)
                    .filter(java.util.Objects::nonNull).max(LocalDateTime::compareTo).orElse(null);
        EmployeeCycleReviewResponse response = EmployeeCycleReviewResponse.builder()
                .reviewId(review.getId()).employeeId(review.getEmployee().getId()).cycleId(cycle.getId())
                .cycleName(cycle.getCycleName())
                .evaluationStartDate(cycle.getEvaluationStartDate())
                .evaluationEndDate(cycle.getEvaluationEndDate())
                .description(cycle.getDescription()).reviewTypeId(cycle.getReviewTypeId())
                .applicableTypeId(cycle.getApplicableTypeId()).scopeValueIds(cycle.getScopeValueIds())
                .cycleStatus(cycle.getStatus())
                .reviewStatus(review.getStatus())
                .assessmentStatus(employeeAssessment == null ? null : employeeAssessment.getStatus())
                .assessorRoleId(employeeAssessment == null || employeeAssessment.getAssessorRole() == null
                        ? null : employeeAssessment.getAssessorRole().getId())
                .assessorRoleName(employeeAssessment == null || employeeAssessment.getAssessorRole() == null
                        ? null : employeeAssessment.getAssessorRole().getName())
                .progressPercentage(employeeProgress).overallProgressPercentage(review.getProgressPercentage())
                .originalDueDate(originalDueDate).dueDate(dueDate)
                .overdue(calculateOverdue(dueDate,
                        employeeAssessment == null ? null : employeeAssessment.getStatus(), LocalDate.now()))
                .extended(extended)
                .extensionDaysPerStage(extended ? review.getExtensionDaysPerStage() : null)
                .extensionReason(extended ? employeeAssessment.getReopenReason() : null)
                .extendedAt(extended ? employeeAssessment.getReopenedDate() : null)
                .startedDate(startedDate).submittedDate(submittedDate)
                .reviewCreatedDate(reviewCreatedDate).reviewUpdatedDate(reviewUpdatedDate).build();
        lookupValueRepository.findById(cycle.getReviewTypeId()).ifPresent(v -> response.setReviewTypeName(v.getName()));
        return response;
    }

    @Override
    public EmployeeReviewResponse startReview(Long employeeId, Long cycleId) {
        EmployeeReview review = findEmployeeReview(employeeId, cycleId);
        EmployeeReviewAssessment assessment = findCurrentAssessment(review, employeeId);
        ensureDeadlineOpen(review, assessment);
        startAssessment(assessment); recalculateReview(review);
        return EmployeeReviewMapper.toResponse(reviewRepository.save(review));
    }

    @Override
    public EmployeeReviewResponse saveAssessment(EmployeeAssessmentRequest request) {
        if (request.getStatus() != EmployeeReviewStatus.IN_PROGRESS && request.getStatus() != EmployeeReviewStatus.SUBMITTED)
            throw new InvalidOperationException("Assessment status must be IN_PROGRESS or SUBMITTED");
        EmployeeReview review = findEmployeeReview(request.getEmployeeId(), request.getCycleId());
        Long actingEmployeeId = request.getAssessorEmployeeId() != null
                ? request.getAssessorEmployeeId() : request.getEmployeeId();
        EmployeeReviewAssessment assessment = request.getAssessmentId() == null
                ? findCurrentAssessment(review, actingEmployeeId)
                : assessmentRepository.findById(request.getAssessmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Employee review assessment not found"));
        if (!assessment.getEmployeeReview().getId().equals(review.getId()))
            throw new InvalidOperationException("Assessment does not belong to the supplied employee and cycle");
        if (request.getAssessorEmployeeId() != null
                && !assessment.getAssessorEmployee().getId().equals(request.getAssessorEmployeeId()))
            throw new InvalidOperationException("Assessment is assigned to a different assessor");
        if (assessment.getStatus() == EmployeeReviewStatus.SUBMITTED)
            throw new InvalidOperationException("Submitted assessments cannot be edited");
        ensurePreviousLevelSubmitted(review, assessment);
        ensureDeadlineOpen(review, assessment);
        startAssessment(assessment);
        boolean selfAssessment = isSelfAssessment(assessment, review.getEmployee().getId());
        if (selfAssessment) {
            if (request.getAnswers() == null)
                throw new InvalidOperationException("answers are required for an employee self-assessment");
            assessment.getAnswers().clear(); assessmentRepository.flush();
            assessment.getAnswers().addAll(saveAnswers(assessment, request.getAnswers()));
        } else if (request.getAnswers() != null && !request.getAnswers().isEmpty()) {
            throw new InvalidOperationException("Question answers are not accepted for reviewer assessments");
        }
        assessment.setOverallRating(request.getOverallRating()); assessment.setOverallComment(request.getOverallComment());
        assessment.setUpdatedBy(request.getUpdatedBy());
        if (request.getStatus() == EmployeeReviewStatus.SUBMITTED) {
            if (selfAssessment) validateMandatoryQuestions(review, assessment);
            assessment.setStatus(EmployeeReviewStatus.SUBMITTED);
            assessment.setProgressPercentage(new BigDecimal("100.00"));
            assessment.setSubmittedDate(LocalDateTime.now());
        } else {
            assessment.setStatus(EmployeeReviewStatus.IN_PROGRESS);
            assessment.setProgressPercentage(selfAssessment
                    ? calculateAssessmentProgress(assessment,
                            activeQuestionCount(review.getPerformanceCycle().getId()))
                    : BigDecimal.ZERO);
        }
        assessmentRepository.save(assessment);
        if (assessment.getStatus() == EmployeeReviewStatus.SUBMITTED) {
            createNextAssessment(review, assessment, request.getUpdatedBy());
            review.getAssessments().stream()
                    .filter(next -> next.getAssessmentLevel() > assessment.getAssessmentLevel())
                    .min(java.util.Comparator.comparing(EmployeeReviewAssessment::getAssessmentLevel))
                    .ifPresent(next -> {
                        applyStageExtension(review, next);
                        assessmentRepository.save(next);
                        emailNotificationService.queueAssessmentReady(review, next);
                    });
        }
        recalculateReview(review);
        return EmployeeReviewMapper.toResponse(reviewRepository.save(review));
    }

    @Override
    public EmployeeReviewResponse updateReviewerAssessment(Long assessorEmployeeId, Long employeeId,
            Long cycleId, ReviewerAssessmentUpdateRequest request) {
        EmployeeReview review = findEmployeeReview(employeeId, cycleId);
        EmployeeReviewAssessment assessment = findCurrentAssessment(review, assessorEmployeeId);
        if (isSelfAssessment(assessment, employeeId))
            throw new InvalidOperationException("Use the employee assessment API for self-assessments");

        return saveAssessment(EmployeeAssessmentRequest.builder()
                .employeeId(employeeId)
                .cycleId(cycleId)
                .assessmentId(assessment.getId())
                .assessorEmployeeId(assessorEmployeeId)
                .status(request.getStatus())
                .answers(List.of())
                .overallRating(request.getOverallRating())
                .overallComment(request.getOverallComment())
                .updatedBy(request.getUpdatedBy() == null
                        ? assessorEmployeeId : request.getUpdatedBy())
                .build());
    }

    private void startAssessment(EmployeeReviewAssessment assessment) {
        if (assessment.getStartedDate() == null) assessment.setStartedDate(LocalDateTime.now());
        if (assessment.getStatus() == EmployeeReviewStatus.NOT_STARTED) assessment.setStatus(EmployeeReviewStatus.IN_PROGRESS);
    }

    private void ensurePreviousLevelSubmitted(EmployeeReview review, EmployeeReviewAssessment current) {
        boolean pending = review.getAssessments().stream()
                .filter(a -> a.getAssessmentLevel() < current.getAssessmentLevel())
                .anyMatch(a -> a.getStatus() != EmployeeReviewStatus.SUBMITTED);
        if (pending) throw new InvalidOperationException("Previous assessment level must be submitted first");
    }

    private void ensureDeadlineOpen(EmployeeReview review, EmployeeReviewAssessment assessment) {
        List<PerformanceCycleTimeline> timelines = timelineRepository
                .findByPerformanceCycleIdOrderByDisplayOrderAsc(review.getPerformanceCycle().getId());
        LocalDate dueDate = effectiveDueDate(assessment, timelines);
        if (calculateOverdue(dueDate, assessment.getStatus(), LocalDate.now())) {
            throw new InvalidOperationException("Assessment deadline has passed; an admin extension is required");
        }
    }

    private List<EmployeeReviewAnswer> saveAnswers(EmployeeReviewAssessment assessment, List<EmployeeReviewAnswerRequest> requests) {
        List<EmployeeReviewAnswer> answers = new ArrayList<>();
        for (EmployeeReviewAnswerRequest request : requests) {
            PerformanceCycleSection section = sectionRepository.findById(request.getSectionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Performance cycle section not found"));
            PerformanceCycleQuestion question = questionRepository.findById(request.getQuestionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Performance cycle question not found"));
            if (!question.getPerformanceCycleSectionId().equals(section.getId()))
                throw new InvalidOperationException("Question does not belong to the supplied section");
            if (!section.getPerformanceCycleId().equals(assessment.getEmployeeReview().getPerformanceCycle().getId()))
                throw new InvalidOperationException("Section does not belong to the review cycle");
            EmployeeReviewAnswer answer = EmployeeReviewMapper.toAnswerEntity(assessment, request);
            answer.setSectionSnapshotName(section.getSectionName());
            answer.setQuestionSnapshotText(question.getQuestionText());
            answer.setResponseTypeSnapshot(question.getResponseType());
            answer.setRequiredSnapshot(question.getRequired());
            answer.setPerformanceCycleSection(section); answer.setPerformanceCycleQuestion(question); answers.add(answer);
        }
        return answerRepository.saveAll(answers);
    }

    private void validateMandatoryQuestions(EmployeeReview review, EmployeeReviewAssessment assessment) {
        List<Long> sectionIds = sectionRepository.findByPerformanceCycleIdOrderByDisplayOrderAsc(review.getPerformanceCycle().getId())
                .stream().map(PerformanceCycleSection::getId).toList();
        List<Long> answered = assessment.getAnswers().stream().filter(this::hasResponse)
                .map(a -> a.getPerformanceCycleQuestion().getId()).toList();
        questionRepository.findAll().stream().filter(q -> sectionIds.contains(q.getPerformanceCycleSectionId()))
                .filter(q -> Boolean.TRUE.equals(q.getRequired()) && Boolean.TRUE.equals(q.getActive()))
                .filter(q -> !answered.contains(q.getId())).findFirst().ifPresent(q -> {
                    throw new InvalidOperationException("Missing required response: " + q.getQuestionText());
                });
    }

    private void recalculateReview(EmployeeReview review) {
        int total = review.getAssessments().size();
        long submitted = review.getAssessments().stream().filter(a -> a.getStatus() == EmployeeReviewStatus.SUBMITTED).count();
        long activeQuestionCount = activeQuestionCount(review.getPerformanceCycle().getId());
        BigDecimal completedLevelUnits = review.getAssessments().stream()
                .map(assessment -> assessmentCompletion(assessment, activeQuestionCount))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        review.setProgressPercentage(total == 0 ? BigDecimal.ZERO
                : completedLevelUnits.multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP)
                    .min(new BigDecimal("100.00")));
        if (total > 0 && submitted == total) review.setStatus(EmployeeReviewStatus.SUBMITTED);
        else if (review.getAssessments().stream().anyMatch(a -> a.getStatus() != EmployeeReviewStatus.NOT_STARTED))
            review.setStatus(EmployeeReviewStatus.IN_PROGRESS);
        else review.setStatus(EmployeeReviewStatus.NOT_STARTED);
    }

    private long activeQuestionCount(Long cycleId) {
        List<Long> sectionIds = sectionRepository.findByPerformanceCycleIdOrderByDisplayOrderAsc(cycleId).stream()
                .filter(section -> Boolean.TRUE.equals(section.getActive()))
                .map(PerformanceCycleSection::getId).toList();
        return questionRepository.findAll().stream()
                .filter(question -> sectionIds.contains(question.getPerformanceCycleSectionId()))
                .filter(question -> Boolean.TRUE.equals(question.getActive())).count();
    }

    private BigDecimal assessmentCompletion(EmployeeReviewAssessment assessment, long activeQuestionCount) {
        if (assessment.getStatus() == EmployeeReviewStatus.SUBMITTED) return BigDecimal.ONE;
        if (activeQuestionCount == 0) return BigDecimal.ZERO;
        long answered = assessment.getAnswers().stream()
                .filter(answer -> answer.getPerformanceCycleQuestion() != null)
                .filter(this::hasResponse)
                .map(answer -> answer.getPerformanceCycleQuestion().getId()).distinct().count();
        return BigDecimal.valueOf(answered)
                .divide(BigDecimal.valueOf(activeQuestionCount), 6, RoundingMode.HALF_UP)
                .min(BigDecimal.ONE);
    }

    private BigDecimal calculateAssessmentProgress(EmployeeReviewAssessment assessment, long activeQuestionCount) {
        return assessmentCompletion(assessment, activeQuestionCount)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private boolean hasResponse(EmployeeReviewAnswer answer) {
        return answer.getRating() != null
                || (answer.getComment() != null && !answer.getComment().isBlank());
    }

    private void createNextAssessment(EmployeeReview review, EmployeeReviewAssessment current, Long createdBy) {
        List<PerformanceCycleAssessor> configs = assessorConfigRepository
                .findByPerformanceCycleIdOrderByDisplayOrderAsc(review.getPerformanceCycle().getId()).stream()
                .filter(config -> Boolean.TRUE.equals(config.getActive()))
                .toList();
        configs.stream()
                .filter(config -> config.getDisplayOrder() > current.getAssessmentLevel())
                .filter(config -> {
                    LookupValue role = lookupValueRepository.findById(config.getRoleId())
                            .orElseThrow(() -> new InvalidOperationException("Invalid assessor role: " + config.getRoleId()));
                    return assigneeResolver.isApplicable(review.getEmployee(), role);
                })
                .findFirst().ifPresent(config -> {
                    if (assessmentRepository.existsByEmployeeReviewIdAndAssessmentLevel(review.getId(), config.getDisplayOrder())) return;
                    LookupValue role = lookupValueRepository.findById(config.getRoleId())
                            .orElseThrow(() -> new InvalidOperationException("Invalid assessor role: " + config.getRoleId()));
                    EmployeeReviewAssessment next = EmployeeReviewAssessment.builder().employeeReview(review)
                            .assessmentLevel(config.getDisplayOrder()).assessorRole(role)
                            .assessorEmployee(assigneeResolver.resolve(review.getEmployee(), role))
                            .status(EmployeeReviewStatus.NOT_STARTED).progressPercentage(BigDecimal.ZERO)
                            .createdBy(createdBy).updatedBy(createdBy).build();
                    assessmentRepository.save(next);
                    review.getAssessments().add(next);
                });
    }

    void applyStageExtension(EmployeeReview review, EmployeeReviewAssessment assessment) {
        Integer daysPerStage = review.getExtensionDaysPerStage();
        if (daysPerStage == null) return;
        assessment.setDueDate(LocalDate.now().plusDays(daysPerStage));
        assessment.setReopenReason(review.getExtensionReason());
        assessment.setReopenedDate(LocalDateTime.now());
    }

    @Override
    public EmployeeReviewResponse updateReview(Long id, EmployeeReviewRequest request) {
        throw new InvalidOperationException("Use POST /api/employee-reviews/assessment for assessment updates");
    }

    @Override
    public EmployeeReviewResponse submitReview(Long id) {
        throw new InvalidOperationException("Submit an individual assessment using POST /api/employee-reviews/assessment");
    }

    private EmployeeReview findReview(Long id) { return reviewRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Employee review not found")); }
    private EmployeeReview findEmployeeReview(Long employeeId, Long cycleId) {
        return reviewRepository.findByEmployeeIdAndPerformanceCycleId(employeeId, cycleId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee review not found for employee " + employeeId + " and cycle " + cycleId));
    }

    private EmployeeReviewAssessment findCurrentAssessment(EmployeeReview review, Long assessorEmployeeId) {
        return assessmentRepository
                .findByEmployeeReviewIdAndAssessorEmployeeIdOrderByAssessmentLevelAsc(
                        review.getId(), assessorEmployeeId).stream()
                .filter(assessment -> assessment.getStatus() != EmployeeReviewStatus.SUBMITTED)
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No active assessment found for employee " + assessorEmployeeId));
    }
}
