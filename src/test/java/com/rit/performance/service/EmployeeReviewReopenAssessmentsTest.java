package com.rit.performance.service;

import com.rit.performance.dto.EmployeeAssessmentRequest;
import com.rit.performance.dto.EmployeeCycleReviewResponse;
import com.rit.performance.dto.ReopenAssessmentsRequest;
import com.rit.performance.dto.ReviewProgressResponse;
import com.rit.performance.entity.Employee;
import com.rit.performance.entity.EmployeeReview;
import com.rit.performance.entity.EmployeeReviewAssessment;
import com.rit.performance.entity.LookupValue;
import com.rit.performance.entity.PerformanceCycleTimeline;
import com.rit.performance.entity.PerformanceCycles;
import com.rit.performance.exception.InvalidOperationException;
import com.rit.performance.exception.ResourceNotFoundException;
import com.rit.performance.repository.EmployeeAssignmentRepository;
import com.rit.performance.repository.EmployeeRepository;
import com.rit.performance.repository.EmployeeReviewAnswerRepository;
import com.rit.performance.repository.EmployeeReviewAssessmentRepository;
import com.rit.performance.repository.EmployeeReviewRepository;
import com.rit.performance.repository.FinalRatingRepository;
import com.rit.performance.repository.LookupValueRepository;
import com.rit.performance.repository.PerformanceCycleAssessorRepository;
import com.rit.performance.repository.PerformanceCycleConfigRepository;
import com.rit.performance.repository.PerformanceCycleQuestionRepository;
import com.rit.performance.repository.PerformanceCycleSectionRepository;
import com.rit.performance.repository.PerformanceCycleTimelineRepository;
import com.rit.performance.repository.SowRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeReviewReopenAssessmentsTest {

    @Mock private EmployeeReviewRepository reviewRepository;
    @Mock private EmployeeReviewAssessmentRepository assessmentRepository;
    @Mock private EmployeeReviewAnswerRepository answerRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private EmployeeAssignmentRepository employeeAssignmentRepository;
    @Mock private SowRepository sowRepository;
    @Mock private FinalRatingRepository finalRatingRepository;
    @Mock private PerformanceCycleTimelineRepository timelineRepository;
    @Mock private PerformanceCycleConfigRepository cycleRepository;
    @Mock private PerformanceCycleQuestionRepository questionRepository;
    @Mock private PerformanceCycleSectionRepository sectionRepository;
    @Mock private LookupValueRepository lookupValueRepository;
    @Mock private PerformanceCycleAssessorRepository assessorConfigRepository;
    @Mock private AssessmentAssigneeResolver assigneeResolver;
    @Mock private EmailNotificationService emailNotificationService;

    private EmployeeReviewServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new EmployeeReviewServiceImpl(reviewRepository, assessmentRepository, answerRepository,
                employeeRepository, employeeAssignmentRepository, sowRepository, finalRatingRepository,
                timelineRepository, cycleRepository, questionRepository, sectionRepository,
                lookupValueRepository, assessorConfigRepository, assigneeResolver, emailNotificationService);
    }

    @Test
    void reopensCurrentOverdueAssessmentForEverySelectedEmployee() {
        Long cycleId = 10L;
        EmployeeReview firstReview = review(101L, submittedAssessment(1), overdueAssessment(42L, 2));
        EmployeeReview secondReview = review(105L, overdueAssessment(44L, 1));
        ReopenAssessmentsRequest request = request(List.of(101L, 105L), true);

        when(cycleRepository.existsById(cycleId)).thenReturn(true);
        when(cycleRepository.findById(cycleId)).thenReturn(Optional.of(
                PerformanceCycles.builder().id(cycleId).cycleName("Annual Review").build()));
        when(reviewRepository.findByPerformanceCycleIdAndEmployeeIdIn(eq(cycleId), anyList()))
                .thenReturn(List.of(firstReview, secondReview));
        when(timelineRepository.findByPerformanceCycleIdOrderByDisplayOrderAsc(cycleId))
                .thenReturn(List.of());
        when(reviewRepository.findByPerformanceCycleId(cycleId)).thenReturn(List.of());

        ReviewProgressResponse response = service.reopenAssessments(cycleId, request);

        assertEquals(cycleId, response.getCycleId());
        LocalDate expectedDueDate = LocalDate.now().plusDays(request.getDaysPerStage());
        assertEquals(expectedDueDate, firstReview.getAssessments().get(1).getDueDate());
        assertEquals(expectedDueDate, secondReview.getAssessments().get(0).getDueDate());
        assertEquals(request.getDaysPerStage(), firstReview.getExtensionDaysPerStage());
        assertEquals(request.getDaysPerStage(), secondReview.getExtensionDaysPerStage());
        assertEquals("Approved extension", firstReview.getAssessments().get(1).getReopenReason());
        verify(reviewRepository).saveAll(anyList());
        verify(assessmentRepository).saveAll(anyList());
        verify(emailNotificationService).queueAssessmentReopened(firstReview,
                firstReview.getAssessments().get(1), expectedDueDate, "Approved extension");
        verify(emailNotificationService).queueAssessmentReopened(secondReview,
                secondReview.getAssessments().get(0), expectedDueDate, "Approved extension");
    }

    @Test
    void rejectsEntireBulkRequestWhenAnyEmployeeIsNotOverdue() {
        Long cycleId = 10L;
        EmployeeReview overdueReview = review(101L, overdueAssessment(42L, 1));
        EmployeeReview activeReview = review(105L, assessment(44L, 1,
                EmployeeReviewStatus.IN_PROGRESS, LocalDate.now().plusDays(1)));
        ReopenAssessmentsRequest request = request(List.of(101L, 105L), false);

        when(cycleRepository.existsById(cycleId)).thenReturn(true);
        when(reviewRepository.findByPerformanceCycleIdAndEmployeeIdIn(eq(cycleId), anyList()))
                .thenReturn(List.of(overdueReview, activeReview));
        when(timelineRepository.findByPerformanceCycleIdOrderByDisplayOrderAsc(cycleId))
                .thenReturn(List.of());

        assertThrows(InvalidOperationException.class,
                () -> service.reopenAssessments(cycleId, request));

        assertEquals(LocalDate.now().minusDays(1), overdueReview.getAssessments().get(0).getDueDate());
        verify(assessmentRepository, never()).saveAll(anyList());
    }

    @Test
    void rejectsEmployeeWithoutAReviewInTheSelectedCycle() {
        Long cycleId = 10L;
        ReopenAssessmentsRequest request = request(List.of(999L), false);

        when(cycleRepository.existsById(cycleId)).thenReturn(true);
        when(reviewRepository.findByPerformanceCycleIdAndEmployeeIdIn(eq(cycleId), anyList()))
                .thenReturn(List.of());
        when(timelineRepository.findByPerformanceCycleIdOrderByDisplayOrderAsc(cycleId))
                .thenReturn(List.of());

        assertThrows(ResourceNotFoundException.class,
                () -> service.reopenAssessments(cycleId, request));
        verify(assessmentRepository, never()).saveAll(anyList());
    }

    @Test
    void givesEachNextStageTheConfiguredNumberOfDaysWhenItBecomesAvailable() {
        EmployeeReview review = review(101L, submittedAssessment(1),
                assessment(44L, 2, EmployeeReviewStatus.NOT_STARTED, null));
        review.setExtensionDaysPerStage(2);
        review.setExtensionReason("Approved extension");
        EmployeeReviewAssessment nextStage = review.getAssessments().get(1);

        service.applyStageExtension(review, nextStage);

        assertEquals(LocalDate.now().plusDays(2), nextStage.getDueDate());
        assertEquals("Approved extension", nextStage.getReopenReason());
    }

    @Test
    void submittingSelfReviewGivesTeamLeadTwoFreshDays() {
        Long cycleId = 10L;
        Employee tl = new Employee();
        tl.setId(201L);
        EmployeeReviewAssessment self = assessment(42L, 1,
                EmployeeReviewStatus.IN_PROGRESS, LocalDate.now().plusDays(1));
        EmployeeReviewAssessment tlStage = assessment(44L, 2,
                EmployeeReviewStatus.NOT_STARTED, null);
        tlStage.setAssessorEmployee(tl);
        EmployeeReview review = review(101L, self, tlStage);
        self.setAssessorEmployee(review.getEmployee());
        review.setId(38L);
        review.setPerformanceCycle(PerformanceCycles.builder().id(cycleId).build());
        review.setExtensionDaysPerStage(2);
        review.setExtensionReason("Approved extension");

        when(reviewRepository.findByEmployeeIdAndPerformanceCycleId(101L, cycleId))
                .thenReturn(Optional.of(review));
        when(assessmentRepository.findById(42L)).thenReturn(Optional.of(self));
        when(timelineRepository.findByPerformanceCycleIdOrderByDisplayOrderAsc(cycleId))
                .thenReturn(List.of());
        when(assessorConfigRepository.findByPerformanceCycleIdOrderByDisplayOrderAsc(cycleId))
                .thenReturn(List.of());
        when(reviewRepository.save(review)).thenReturn(review);

        service.saveAssessment(EmployeeAssessmentRequest.builder()
                .employeeId(101L).cycleId(cycleId).assessmentId(42L)
                .status(EmployeeReviewStatus.SUBMITTED)
                .answers(List.of()).updatedBy(201L).build());

        assertEquals(EmployeeReviewStatus.SUBMITTED, self.getStatus());
        assertEquals(LocalDate.now().plusDays(2), tlStage.getDueDate());
        verify(assessmentRepository).save(tlStage);
        verify(emailNotificationService).queueAssessmentReady(review, tlStage);
    }

    @Test
    void submittingSelfReviewSkipsUnavailableLeadAndPublishOnlyHr() {
        Long cycleId = 15L;
        EmployeeReviewAssessment self = assessment(74L, 1,
                EmployeeReviewStatus.IN_PROGRESS, LocalDate.now().plusDays(1));
        EmployeeReview review = review(2L, self);
        self.setAssessorEmployee(review.getEmployee());
        review.setId(40L);
        review.setPerformanceCycle(PerformanceCycles.builder().id(cycleId).build());
        LookupValue lead = LookupValue.builder().id(30L).code("LEAD").name("Team Lead").build();
        LookupValue hr = LookupValue.builder().id(32L).code("HR").name("HR").build();
        com.rit.performance.entity.PerformanceCycleAssessor leadConfig =
                com.rit.performance.entity.PerformanceCycleAssessor.builder()
                        .performanceCycleId(cycleId).roleId(30L).displayOrder(2).active(true).build();
        com.rit.performance.entity.PerformanceCycleAssessor hrConfig =
                com.rit.performance.entity.PerformanceCycleAssessor.builder()
                        .performanceCycleId(cycleId).roleId(32L).displayOrder(4).active(true).build();

        when(reviewRepository.findByEmployeeIdAndPerformanceCycleId(2L, cycleId))
                .thenReturn(Optional.of(review));
        when(assessmentRepository.findById(74L)).thenReturn(Optional.of(self));
        when(timelineRepository.findByPerformanceCycleIdOrderByDisplayOrderAsc(cycleId))
                .thenReturn(List.of());
        when(assessorConfigRepository.findByPerformanceCycleIdOrderByDisplayOrderAsc(cycleId))
                .thenReturn(List.of(leadConfig, hrConfig));
        when(lookupValueRepository.findById(30L)).thenReturn(Optional.of(lead));
        when(lookupValueRepository.findById(32L)).thenReturn(Optional.of(hr));
        when(assigneeResolver.isApplicable(review.getEmployee(), lead)).thenReturn(true);
        when(assigneeResolver.resolveIfAvailable(review.getEmployee(), lead)).thenReturn(Optional.empty());
        when(assigneeResolver.isPublishOnly(any(LookupValue.class)))
                .thenAnswer(invocation -> "HR".equals(
                        ((LookupValue) invocation.getArgument(0)).getCode()));
        when(reviewRepository.save(review)).thenReturn(review);

        service.saveAssessment(EmployeeAssessmentRequest.builder()
                .employeeId(2L).cycleId(cycleId).assessmentId(74L).assessorEmployeeId(2L)
                .status(EmployeeReviewStatus.SUBMITTED).answers(List.of()).updatedBy(1L).build());

        assertEquals(EmployeeReviewStatus.SUBMITTED, self.getStatus());
        assertEquals(EmployeeReviewStatus.SUBMITTED, review.getStatus());
        assertEquals(1, review.getAssessments().size());
    }

    @Test
    void employeeCycleResponseDistinguishesOriginalAndExtendedDeadlines() {
        Long cycleId = 6L;
        LocalDateTime extendedAt = LocalDateTime.of(2026, 8, 3, 5, 38);
        LookupValue employeeRole = LookupValue.builder().id(29L)
                .code("EMPLOYEE").name("Employee").build();
        EmployeeReviewAssessment self = assessment(2L, 1,
                EmployeeReviewStatus.IN_PROGRESS, LocalDate.of(2026, 8, 5));
        self.setAssessorRole(employeeRole);
        self.setReopenReason("Approved extension");
        self.setReopenedDate(extendedAt);
        EmployeeReview review = review(2L, self);
        self.setAssessorEmployee(review.getEmployee());
        review.setId(2L);
        review.setExtensionDaysPerStage(2);
        review.setPerformanceCycle(PerformanceCycles.builder().id(cycleId)
                .cycleName("RIT 2026 - June | Reviews")
                .evaluationStartDate(LocalDate.of(2026, 7, 1))
                .evaluationEndDate(LocalDate.of(2026, 7, 31))
                .reviewTypeId(1L).build());
        PerformanceCycleTimeline timeline = PerformanceCycleTimeline.builder()
                .performanceCycleId(cycleId).roleId(29L).displayOrder(1)
                .endDate(LocalDate.of(2026, 7, 19)).build();

        when(employeeRepository.existsById(2L)).thenReturn(true);
        when(reviewRepository.findByEmployeeIdOrderByCreatedDateDesc(2L))
                .thenReturn(List.of(review));
        when(timelineRepository.findByPerformanceCycleIdInOrderByPerformanceCycleIdAscDisplayOrderAsc(
                List.of(cycleId))).thenReturn(List.of(timeline));

        EmployeeCycleReviewResponse response = service.getEmployeeCycles(2L).get(0);

        assertEquals(LocalDate.of(2026, 7, 19), response.getOriginalDueDate());
        assertEquals(LocalDate.of(2026, 8, 5), response.getDueDate());
        assertEquals(LocalDate.of(2026, 7, 1), response.getEvaluationStartDate());
        assertEquals(LocalDate.of(2026, 7, 31), response.getEvaluationEndDate());
        assertEquals(true, response.isExtended());
        assertEquals(2, response.getExtensionDaysPerStage());
        assertEquals("Approved extension", response.getExtensionReason());
        assertEquals(extendedAt, response.getExtendedAt());
    }

    private ReopenAssessmentsRequest request(List<Long> employeeIds, boolean notifyAssignees) {
        ReopenAssessmentsRequest request = new ReopenAssessmentsRequest();
        request.setEmployeeIds(employeeIds);
        request.setDaysPerStage(2);
        request.setReason(" Approved extension ");
        request.setNotifyAssignees(notifyAssignees);
        return request;
    }

    private EmployeeReview review(Long employeeId, EmployeeReviewAssessment... assessments) {
        Employee employee = new Employee();
        employee.setId(employeeId);
        EmployeeReview review = EmployeeReview.builder().employee(employee)
                .assessments(new ArrayList<>()).build();
        for (EmployeeReviewAssessment assessment : assessments) {
            assessment.setEmployeeReview(review);
            review.getAssessments().add(assessment);
        }
        return review;
    }

    private EmployeeReviewAssessment submittedAssessment(int level) {
        return assessment((long) level, level, EmployeeReviewStatus.SUBMITTED,
                LocalDate.now().minusDays(2));
    }

    private EmployeeReviewAssessment overdueAssessment(Long id, int level) {
        return assessment(id, level, EmployeeReviewStatus.IN_PROGRESS,
                LocalDate.now().minusDays(1));
    }

    private EmployeeReviewAssessment assessment(Long id, int level, EmployeeReviewStatus status,
            LocalDate dueDate) {
        return EmployeeReviewAssessment.builder().id(id).assessmentLevel(level)
                .status(status).dueDate(dueDate).build();
    }
}
