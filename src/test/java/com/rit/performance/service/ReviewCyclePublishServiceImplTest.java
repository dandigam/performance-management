package com.rit.performance.service;

import com.rit.performance.entity.*;
import com.rit.performance.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class ReviewCyclePublishServiceImplTest {
    @Mock private PerformanceCycleConfigRepository cycleRepository;
    @Mock private PerformanceCycleSectionRepository sectionRepository;
    @Mock private PerformanceCycleQuestionRepository questionRepository;
    @Mock private LookupValueRepository lookupValueRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private EmployeeAssignmentRepository employeeAssignmentRepository;
    @Mock private EmployeeReviewRepository reviewRepository;
    @Mock private EmployeeReviewAssessmentRepository assessmentRepository;
    @Mock private PerformanceCycleAssessorRepository assessorConfigRepository;
    @Mock private AssessmentAssigneeResolver assigneeResolver;
    @Mock private EmailNotificationService emailNotificationService;

    private ReviewCyclePublishServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ReviewCyclePublishServiceImpl(
                cycleRepository, sectionRepository, questionRepository, lookupValueRepository,
                employeeRepository, employeeAssignmentRepository, reviewRepository,
                assessmentRepository, assessorConfigRepository, assigneeResolver,
                emailNotificationService);
    }

    @Test
    void treatsHrAsPublishOnlyWithoutRequiringAnEmployeeLink() {
        PerformanceCycles cycle = PerformanceCycles.builder()
                .id(15L).status("DRAFT").applicableTypeId(1L).build();
        Employee employee = new Employee();
        employee.setId(3L);
        employee.setFirstName("Putsala");
        EmployeeReview review = EmployeeReview.builder()
                .id(30L).employee(employee).performanceCycle(cycle).build();
        PerformanceCycleSection section = PerformanceCycleSection.builder().id(2L).build();
        PerformanceCycleQuestion question = PerformanceCycleQuestion.builder().id(3L).build();
        LookupValue all = LookupValue.builder().id(1L).code("ALL").name("All").build();
        LookupValue hr = LookupValue.builder().id(40L).code("HR").name("HR").build();
        PerformanceCycleAssessor hrConfig = PerformanceCycleAssessor.builder()
                .id(5L).performanceCycleId(15L).roleId(40L).displayOrder(4).active(true).build();

        when(cycleRepository.findById(15L)).thenReturn(Optional.of(cycle));
        when(sectionRepository.findByPerformanceCycleIdOrderByDisplayOrderAsc(15L))
                .thenReturn(List.of(section));
        when(questionRepository.findByPerformanceCycleSectionIdOrderByDisplayOrderAsc(2L))
                .thenReturn(List.of(question));
        when(lookupValueRepository.findById(1L)).thenReturn(Optional.of(all));
        when(employeeRepository.findAll()).thenReturn(List.of(employee));
        when(reviewRepository.findByPerformanceCycleId(15L)).thenReturn(List.of(review));
        when(assessorConfigRepository.findByPerformanceCycleIdOrderByDisplayOrderAsc(15L))
                .thenReturn(List.of(hrConfig));
        when(assessmentRepository.existsByEmployeeReviewIdAndAssessmentLevel(30L, 4))
                .thenReturn(false);
        when(lookupValueRepository.findById(40L)).thenReturn(Optional.of(hr));
        when(assigneeResolver.isPublishOnly(hr)).thenReturn(true);

        var response = service.publish(15L, 3L);

        assertEquals("PUBLISHED", response.getCycleStatus());
        assertEquals(0, response.getAssessmentsCreated());
        verify(assessmentRepository).saveAll(List.of());
        verify(assigneeResolver, never()).resolve(employee, hr);
    }

    @Test
    void keepsAllEmployeeAndSkipsOnlyUnavailableTeamLeadStage() {
        PerformanceCycles cycle = PerformanceCycles.builder()
                .id(16L).status("DRAFT").applicableTypeId(1L).build();
        Employee employee = new Employee();
        employee.setId(3L);
        employee.setFirstName("Putsala");
        EmployeeReview review = EmployeeReview.builder()
                .id(31L).employee(employee).performanceCycle(cycle).build();
        PerformanceCycleSection section = PerformanceCycleSection.builder().id(2L).build();
        LookupValue all = LookupValue.builder().id(1L).code("ALL").name("All").build();
        LookupValue self = LookupValue.builder().id(29L).code("EMPLOYEE").name("Employee").build();
        LookupValue lead = LookupValue.builder().id(30L).code("LEAD").name("Team Lead").build();
        PerformanceCycleAssessor selfConfig = PerformanceCycleAssessor.builder()
                .performanceCycleId(16L).roleId(29L).displayOrder(1).active(true).build();
        PerformanceCycleAssessor leadConfig = PerformanceCycleAssessor.builder()
                .performanceCycleId(16L).roleId(30L).displayOrder(2).active(true).build();

        when(cycleRepository.findById(16L)).thenReturn(Optional.of(cycle));
        when(sectionRepository.findByPerformanceCycleIdOrderByDisplayOrderAsc(16L))
                .thenReturn(List.of(section));
        when(questionRepository.findByPerformanceCycleSectionIdOrderByDisplayOrderAsc(2L))
                .thenReturn(List.of(PerformanceCycleQuestion.builder().id(3L).build()));
        when(lookupValueRepository.findById(1L)).thenReturn(Optional.of(all));
        when(employeeRepository.findAll()).thenReturn(List.of(employee));
        when(reviewRepository.findByPerformanceCycleId(16L)).thenReturn(List.of(review));
        when(assessorConfigRepository.findByPerformanceCycleIdOrderByDisplayOrderAsc(16L))
                .thenReturn(List.of(selfConfig, leadConfig));
        when(assessmentRepository.existsByEmployeeReviewIdAndAssessmentLevel(31L, 1)).thenReturn(false);
        when(assessmentRepository.existsByEmployeeReviewIdAndAssessmentLevel(31L, 2)).thenReturn(false);
        when(lookupValueRepository.findById(29L)).thenReturn(Optional.of(self));
        when(lookupValueRepository.findById(30L)).thenReturn(Optional.of(lead));
        when(assigneeResolver.isApplicable(employee, self)).thenReturn(true);
        when(assigneeResolver.isApplicable(employee, lead)).thenReturn(true);
        when(assigneeResolver.resolveIfAvailable(employee, self)).thenReturn(Optional.of(employee));
        when(assigneeResolver.resolveIfAvailable(employee, lead)).thenReturn(Optional.empty());

        var response = service.publish(16L, 3L);

        assertEquals(1, response.getEligibleEmployees());
        assertEquals(1, response.getAssessmentsCreated());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<EmployeeReviewAssessment>> captor = ArgumentCaptor.forClass(List.class);
        verify(assessmentRepository).saveAll(captor.capture());
        assertEquals(1, captor.getValue().size());
        assertEquals(29L, captor.getValue().get(0).getAssessorRole().getId());
    }
}
