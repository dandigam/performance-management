package com.rit.performance.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rit.performance.dto.FinalRatingResponse;
import com.rit.performance.entity.EmployeeReview;
import com.rit.performance.entity.EmployeeReviewAssessment;
import com.rit.performance.entity.Employee;
import com.rit.performance.entity.FinalRating;
import com.rit.performance.entity.PerformanceCycles;
import com.rit.performance.entity.User;
import com.rit.performance.repository.EmployeeRepository;
import com.rit.performance.repository.EmployeeReviewRepository;
import com.rit.performance.repository.FinalRatingRepository;
import com.rit.performance.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinalRatingServiceImplTest {

    @Mock private FinalRatingRepository finalRatingRepository;
    @Mock private EmployeeReviewRepository employeeReviewRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private UserRepository userRepository;
    @Mock private EmailNotificationService emailNotificationService;

    private FinalRatingServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FinalRatingServiceImpl(finalRatingRepository, employeeReviewRepository,
                employeeRepository, userRepository, emailNotificationService);
    }

    @Test
    void publishesSelfRatingWhenSelfIsTheOnlyConfiguredStage() {
        EmployeeReview review = submittedReview(54L, List.of(submittedAssessment(1, "5.00")));
        preparePublish(review);

        FinalRatingResponse response = service.publishRating(54L, 3L);

        assertEquals(new BigDecimal("5.00"), response.getFinalRating());
    }

    @Test
    void publishesRatingFromHighestConfiguredStage() {
        EmployeeReview review = submittedReview(55L, List.of(
                submittedAssessment(1, "3.00"),
                submittedAssessment(2, "4.00"),
                submittedAssessment(3, "4.50")));
        preparePublish(review);

        FinalRatingResponse response = service.publishRating(55L, 3L);

        assertEquals(new BigDecimal("4.50"), response.getFinalRating());
    }

    @Test
    void returnsAllEmployeesAndGroupsTheirPerformanceHistory() {
        Employee employee = new Employee();
        employee.setId(4L);
        employee.setFirstName("Dinakar");
        employee.setLastName("kalaga");
        Employee unratedEmployee = new Employee();
        unratedEmployee.setId(5L);
        unratedEmployee.setFirstName("Srini");
        unratedEmployee.setLastName("N");
        FinalRating june = rating(3L, 8L, employee, 6L, "RIT 2026 - June | Reviews", "2.0");
        FinalRating july = rating(4L, 43L, employee, 12L, "July Reviews", "2.0");
        FinalRating august = rating(5L, 48L, employee, 13L,
                "RIT 2026 - Test Reviews for August Period", "2.0");
        when(finalRatingRepository.findAll()).thenReturn(List.of(june, july, august));
        when(employeeRepository.findAll()).thenReturn(List.of(employee, unratedEmployee));

        List<FinalRatingResponse> response = service.getAllFinalRatings();

        assertEquals(2, response.size());
        assertEquals(4L, response.get(0).getEmployeeId());
        assertEquals(3, response.get(0).getPerformance().size());
        assertEquals(List.of(6L, 12L, 13L), response.get(0).getPerformance().stream()
                .map(FinalRatingResponse::getCycleId)
                .toList());
        assertEquals(5L, response.get(1).getEmployeeId());
        assertEquals("Srini N", response.get(1).getEmployeeName());
        assertEquals(List.of(), response.get(1).getPerformance());
        assertEquals("[]", new ObjectMapper().valueToTree(response.get(1))
                .get("performance").toString());
    }

    private void preparePublish(EmployeeReview review) {
        User publisher = new User();
        publisher.setId(3L);
        publisher.setUsername("admin");
        publisher.setStatus("ACTIVE");
        when(employeeReviewRepository.findById(review.getId())).thenReturn(Optional.of(review));
        when(userRepository.findById(3L)).thenReturn(Optional.of(publisher));
        when(finalRatingRepository.findByEmployeeReviewId(review.getId())).thenReturn(Optional.empty());
        when(finalRatingRepository.save(any(FinalRating.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private EmployeeReview submittedReview(Long id, List<EmployeeReviewAssessment> assessments) {
        EmployeeReview review = EmployeeReview.builder()
                .id(id)
                .status(EmployeeReviewStatus.SUBMITTED)
                .assessments(new ArrayList<>(assessments))
                .build();
        review.getAssessments().forEach(assessment -> assessment.setEmployeeReview(review));
        return review;
    }

    private EmployeeReviewAssessment submittedAssessment(int level, String rating) {
        return EmployeeReviewAssessment.builder()
                .assessmentLevel(level)
                .status(EmployeeReviewStatus.SUBMITTED)
                .overallRating(new BigDecimal(rating))
                .build();
    }

    private FinalRating rating(Long id, Long reviewId, Employee employee, Long cycleId,
            String cycleName, String value) {
        EmployeeReview review = EmployeeReview.builder()
                .id(reviewId)
                .employee(employee)
                .performanceCycle(PerformanceCycles.builder().id(cycleId).cycleName(cycleName).build())
                .build();
        return FinalRating.builder()
                .id(id)
                .employeeReview(review)
                .finalRating(new BigDecimal(value))
                .published(true)
                .build();
    }
}
